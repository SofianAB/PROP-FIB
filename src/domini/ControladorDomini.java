package domini;

import dades.ControladorDades;

import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ControladorDomini implements Cloneable
{
    private String nomCentre;
    private PeriodeLectiu periodeLectiu;
    private JornadaLectiva jornadaLectiva;
    private Aules aules;
    private PlansDeEstudis plansDeEstudis;
    private Horari horari;
    private ControladorDades controladorDades;

    public ControladorDomini() throws IOException {
        this.controladorDades   = new ControladorDades();
        this.nomCentre          = new String();
        this.periodeLectiu      = new PeriodeLectiu();
        this.jornadaLectiva     = new JornadaLectiva();
        this.aules              = new Aules();
        this.plansDeEstudis     = new PlansDeEstudis();
        this.horari             = new Horari();
    }

    public ControladorDomini(String nomCentre, PeriodeLectiu periodeLectiu, JornadaLectiva jornadaLectiva) throws IOException {
        this.controladorDades   = new ControladorDades();
        this.nomCentre          = nomCentre;
        this.periodeLectiu      = new PeriodeLectiu(periodeLectiu);
        this.jornadaLectiva     = new JornadaLectiva(jornadaLectiva);
        this.aules              = new Aules();
        this.plansDeEstudis     = new PlansDeEstudis();
        this.horari             = new Horari();
    }

    public ControladorDomini(String nomCentre, PeriodeLectiu periodeLectiu, JornadaLectiva jornadaLectiva, Aules aules, PlansDeEstudis plansDeEstudis) throws IOException {
        this.controladorDades   = new ControladorDades();
        this.nomCentre          = nomCentre;
        this.periodeLectiu      = new PeriodeLectiu(periodeLectiu);
        this.jornadaLectiva     = new JornadaLectiva(jornadaLectiva);
        this.aules              = new Aules(aules);
        this.plansDeEstudis     = new PlansDeEstudis(plansDeEstudis);
        this.horari             = new Horari();
    }

    public ControladorDomini(ControladorDomini cd) {
        this.controladorDades = cd.getControladorDades();
        this.nomCentre      = cd.getNomCentre();
        this.periodeLectiu  = new PeriodeLectiu(cd.getPeriodeLectiu());
        this.jornadaLectiva = new JornadaLectiva(cd.getJornadaLectiva());
        this.aules          = new Aules(cd.getAules());
        this.plansDeEstudis = new PlansDeEstudis(cd.getPlansDeEstudis());
        this.horari         = new Horari(cd.getHorari());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        ControladorDomini cd;
        try {
            cd = (ControladorDomini) super.clone();

            // mutable methods!
            cd.setPeriodeLectiu((PeriodeLectiu) this.getPeriodeLectiu().clone());
            cd.setJornadaLectiva((JornadaLectiva) this.getJornadaLectiva().clone());
            cd.setAules((Aules) this.getAules().clone());
            cd.setPlansDeEstudis((PlansDeEstudis) this.getPlansDeEstudis().clone());
            cd.setHorari((Horari) this.getHorari().clone());
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return cd;
    }

    public void loadDataAll() throws IOException, MyException {
        String centreDocent = controladorDades.loadCentreDocent();
        ArrayList<String> plansDeEstudis = controladorDades.loadPlansDeEstudis();
        ArrayList<String> aules = controladorDades.loadAules();
        ArrayList<String> assignatures = controladorDades.loadAssignatures();

        if(centreDocent != null) Parser.centreDocent(centreDocent, this);
        
        for (String str : plansDeEstudis) {
            PlaEstudis plaEstudis = Parser.plaEstudis(str, this.jornadaLectiva);
            this.plansDeEstudis.afegirPlaEstudis(plaEstudis);
        }

        for (String str : aules) {
            Aula aula = Parser.aula(str);
            this.aules.afegirAula(aula);
        }

        for (String str : assignatures) {
            String nomPlaEstudis = str.split(", ")[1];
            Assignatura assignatura = Parser.assignatura(str);
            this.plansDeEstudis.getPlaEstudis(nomPlaEstudis).afegirAssignaturaAlPlaEstudis(assignatura);
        }
    }

    public void storeDataCentreDocent() throws IOException {
        String centreDocent = Serializer.centreDocent(this);
        controladorDades.saveCentreDocent(centreDocent);
    }

    public void storeDataPlansDeEstudis() throws IOException, MyException {
        ArrayList<String> plansDeEstudis = Serializer.plansDeEstudis(this.plansDeEstudis);
        controladorDades.savePlansDeEstudis(plansDeEstudis);
    }

    public void storeDataAules() throws IOException, InterruptedException {
        ArrayList<String> aules = Serializer.aules(this.aules);
        controladorDades.saveAules(aules);
    }

    public void storeDataAssignatures() throws IOException, MyException {
         for (PlaEstudis pe : this.plansDeEstudis.getPlansDeEstudis()) {
            ArrayList<String> assignatures = Serializer.assignatures(pe.getAssignatures(), pe.getNomPla());
            controladorDades.saveAssignatures(assignatures);
         }
    }

    public boolean generateHorariPlaEstudis(int numPla) throws CloneNotSupportedException, MyException {
        horari = new Horari(this.getPlaEstudis(numPla).getJornadaLectiva(), this.aules.mida());
        boolean sol = horari.generarHorari(this.plansDeEstudis.getPlaEstudis(numPla).getAssignatures(), this.aules);
        this.guardarHorariAlPlaEstudis(numPla);
        return sol;
    }
    
    public boolean swapHorariPla(int dI,int hI, int aI, int dF, int hF, int aF, int numPla) throws CloneNotSupportedException {
        boolean ok = horari.swapAssignacions(dI, hI, aI, dF, hF, aF);
        if(ok) this.guardarHorariAlPlaEstudis(numPla);
        return ok;
    }

    /**
     * @param numPla
     * @throws CloneNotSupportedException
     *
     * Cal cridar sempre que es modifiqui l'horari del ControladorDomini
     *
     */
    private void guardarHorariAlPlaEstudis(int numPla) throws CloneNotSupportedException {
        if (!horari.empty()) {
            this.plansDeEstudis.getPlaEstudis(numPla).setHorari((Horari) this.horari.clone());
        }
    }

    private void carregarHorariDePlaEstudis(int numPla) throws CloneNotSupportedException, MyException {
        if (numPla >= this.plansDeEstudis.mida()) { throw new MyException("No existeix el pla d'estudis num " + numPla + "."); }
        if (this.horari == null) throw new MyException("Encara no s'ha genereat l'horari per aquest pla d'estudis.");
        this.horari = (Horari) this.plansDeEstudis.getPlaEstudis(numPla).getHorari().clone();
    }

    public boolean afegirPlaEstudis(PlaEstudis pe) throws MyException {
        return this.plansDeEstudis.afegirPlaEstudis(pe);
    }

    public boolean eliminarPlaEstudis(PlaEstudis pe) throws MyException {
        return this.plansDeEstudis.eliminarPlaEstudis(pe);
    }

    public boolean afegirAula(Aula a) throws MyException { return this.aules.afegirAula(a); }

    public boolean eliminarAula(Aula a) throws MyException { return this.aules.eliminarAula(a); }

    public void setNomCentre(String nomCentre) {
        this.nomCentre = new String(nomCentre);
    }

    public void setPeriodeLectiu(PeriodeLectiu periodeLectiu) {
        this.periodeLectiu = new PeriodeLectiu(periodeLectiu);
    }

    public void setJornadaLectiva(JornadaLectiva jornadaLectiva) {
        this.jornadaLectiva = new JornadaLectiva(jornadaLectiva);
    }

    public void setPlansDeEstudis(PlansDeEstudis plansDeEstudis) {
        this.plansDeEstudis = plansDeEstudis;
    }

    public void setAules(Aules aules) {
        this.aules = aules;
    }

    public void setHorari(Horari horari) {
        this.horari = horari;
    }

    public void setControladorDades(ControladorDades controladorDades) {
        this.controladorDades = controladorDades;
    }

    public String getNomCentre() {
        return this.nomCentre;
    }

    public PeriodeLectiu getPeriodeLectiu() {
        return this.periodeLectiu;
    }

    public JornadaLectiva getJornadaLectiva() {
        return this.jornadaLectiva;
    }

    public PlansDeEstudis getPlansDeEstudis() {
        return this.plansDeEstudis;
    }

    public PlaEstudis getPlaEstudis(String nomPla) {
        for (int i = 0; i < this.plansDeEstudis.mida(); i++) {
            if (this.plansDeEstudis.getPlaEstudis(i).getNomPla().equals(nomPla)) return this.plansDeEstudis.getPlaEstudis(i);
        }
        return null;
    }

    public PlaEstudis getPlaEstudis(int i) {
        return this.plansDeEstudis.getPlaEstudis(i);
    }

    public Aules getAules() {
        return aules;
    }

    public int getNumAules() {
        return this.aules.mida();
    }

    /**
     * setter method
     *
     */
    public Horari getHorari() { return this.horari; }

    /**
     * funcio per comunicarse amb la capa de presentacio
     *
     */
    public String[][] getHorari(int numPla) throws CloneNotSupportedException, MyException {
        this.carregarHorariDePlaEstudis(numPla);
        String[][] horari = Serializer.horari(this.horari.getHorari(), this.aules);
        return horari;
    }

    public ControladorDades getControladorDades() {
        return controladorDades;
    }

    /**
     *
     * FUNCIONS PER COMUNICAR-SE AMB PRESENTACIÓ
     *
     */
    public boolean generarHorari() throws CloneNotSupportedException, MyException {
        boolean generat = this.generateHorariPlaEstudis(0);
        return generat;
    }

    public void crearAssig(String line) throws MyException {
        Assignatura assignatura = Parser.assignatura(line);
        this.plansDeEstudis.getPlaEstudis(line.split(", ")[1]).afegirAssignaturaAlPlaEstudis(assignatura);
    }

    public void modificarAula(String codi, String nom, String capacitat, String laboratori) {
        int mida = this.aules.mida();
        for (int i = 0; i < mida; ++i){
            if (aules.getAula(i).getCodi().equals(codi)){
                if ("".equals(nom)) {
                }
                else {
                    aules.getAula(i).setCodi(nom);
                }
                if ("".equals(capacitat)) {
                } else {
                    aules.getAula(i).setCapacitat(Integer.parseInt(capacitat));
                }
                if ("".equals(laboratori)) {
                } else {
                    aules.getAula(i).setPCs(Boolean.parseBoolean(laboratori));
                }
            }
        }
    }

    public void eliminarAula(String codi)  {
        this.aules.eliminarAula(codi);
    }
    
    public void eliminarAssig(String codi) {
        this.plansDeEstudis.getPlaEstudis(0).getAssignatures().eliminarAssignatura(codi);
    }

    public void crearAula(String nom, String capacitat, String laboratori) throws MyException{
        Aula a = new Aula(nom, Integer.parseInt(capacitat), Boolean.parseBoolean(laboratori));
        this.aules.afegirAula(a);
    }

    public String getCapacitatAula(String codi) {
        for (int i = 0; i < aules.mida(); ++i){
           if (aules.getAula(i).getCodi().equals(codi)){
               return Integer.toString(aules.getAula(i).getCapacitat());
           }
        }
        return "";
    }

    public String getLab(String codi){
       for (int i = 0; i < aules.mida(); ++i) {
           if (aules.getAula(i).getCodi().equals(codi)){
               return Boolean.toString(aules.getAula(i).isLab());
           }
       }
       return "";
    }

    public String[] getNomAules(){
        int mida = this.aules.mida();
        String[] noms;
        noms = new String[mida];

        for (int i = 0; i < mida; ++i){
            noms[i] = (aules.getAula(i).getCodi());
        }
        return noms;
    }
    
    public String[] getCodiAssigs(int numPla) {
        if (this.existeixPla(numPla)) {
            int mida = this.plansDeEstudis.getPlaEstudis(0).quantesAssignatures();
            String[] noms = new String[mida];
            for (int i = 0; i < mida; ++i) {
                noms[i] = (this.plansDeEstudis.getPlaEstudis(0).getAssignatures().getAssignatura(i).getCodi());
            }
            return noms;
        }
        else return null;
    }
    
    public String getNomAssig(String codi){
        if (existeixPla(0)) return this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getNom();
        return "";
    }

    public String getCredits(String codi){
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getCredits());
        return "";
    }

    public String getCapacitatAssig(String codi) {
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getCapacitatAssignatura());
        return "";
    }

    public String getGrups(String codi){
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getNumGrupsGenerals());
        return "";
    }

    public String getSubgrups(String codi){
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getNumSubGrupsXGrup());
        return "";
    }

    public String getNivell(String codi){
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getNivell());
        return "";
    }

    public String getALab(String codi){
        if (existeixPla(0)) return String.valueOf(this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).teLabAmbPCs());
        return "";
    }

    public String getCorreq(String codi){
        if (existeixPla(0)) {
            ArrayList<String> s = this.plansDeEstudis.getPlaEstudis(0).getAssignatura(codi).getCorrequisits();
            String s2 = "";
            for (int i = 0; i < s.size(); ++i) {
                String s3 = s.get(i);
                s2 = s2.concat(s3);
                if (i != s.size() - 1) s2 = s2 + ", ";
            }
            return s2;
        }
        return "";
    }

    public String getHoraIni(){
        return Serializer.time(this.jornadaLectiva.getHoraIni());
    }
    
    public int getHoraIniInteger(){
        String[] hourMin = this.getHoraIni().split(":");
        int hour = Integer.parseInt(hourMin[0]);
        return hour;
    }
    
    public String getHoraFi(){
        return Serializer.time(this.jornadaLectiva.getHoraFi());
    }
    
    public String getDataIni(){
        return Serializer.date(this.periodeLectiu.getDataIni());
    }

    public String getDataFi(){
        return Serializer.date(this.periodeLectiu.getDataFi());
    }

    public void modificarCalendari(String jornada, String periode) throws ParseException{
        String s1, s2, s3, s4;
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        DateFormat formatter2 = new SimpleDateFormat("dd/MM/yy");
        Time t1, t2;
        Date d1, d2;
        if (!"".equals(jornada)){
            s1 = jornada.substring(0, 5);
            s2 = jornada.substring(6, 11);
            t1 = new Time(formatter.parse(s1).getTime());
            t2 = new Time(formatter.parse(s2).getTime());
            this.jornadaLectiva.setHoraIni(t1);
            this.jornadaLectiva.setHoraFi(t2);
        }
        if (!"".equals(periode)){
            s3 = periode.substring(0, 8);
            s4 = periode.substring(9, 17);
            d1 = formatter2.parse(s3);
            d2 = formatter2.parse(s4);
            this.periodeLectiu.setDataIni(d1);
            this.periodeLectiu.setDataFi(d2);
        }
    }

    public boolean existeixPla(int numPla) {
        if (numPla >= this.plansDeEstudis.getPlansDeEstudis().size()) return false;
        return true;
    }

    public void afegirPla(String nom, String titulacio, String tipus) throws MyException {
        Titulacio t = new Titulacio(titulacio, tipus);
        PlaEstudis pe = new PlaEstudis(nom, this.jornadaLectiva, t);
        this.plansDeEstudis.afegirPlaEstudis(pe);
    }

    public void modificarPla(String nom, String titulacio, String tipus) {
        this.plansDeEstudis.getPlaEstudis(0).setNomPla(nom);
        this.plansDeEstudis.getPlaEstudis(0).getTitulacio().setNom(titulacio);
        this.plansDeEstudis.getPlaEstudis(0).getTitulacio().setTipus(tipus);
    }

    public String getNomPla(){
        return this.plansDeEstudis.getPlaEstudis(0).getNomPla();
    }

    public String getNomTitulacio(){
        return this.plansDeEstudis.getPlaEstudis(0).getTitulacio().getNomTitulacio();
    }

    public String getTipusTitulacio(){
        return this.plansDeEstudis.getPlaEstudis(0).getTitulacio().getTipusTitulacio();
    }

    public void importDataAssignatures(ArrayList<String> assignatures) throws MyException {
        for (String str : assignatures) {
            String nomPlaEstudis = str.split(", ")[1];
            Assignatura assignatura = Parser.assignatura(str);
            this.plansDeEstudis.getPlaEstudis(nomPlaEstudis).afegirAssignaturaAlPlaEstudis(assignatura);
        }
    }
    
    public void importDataAules(ArrayList<String> aules) throws MyException {
        for (String str : aules) {
             Aula aula = Parser.aula(str);
             this.aules.afegirAula(aula);
        }
    }

    public void importDataPlansDeEstudis(ArrayList<String> plansDeEstudis) throws MyException {
        for (String str : plansDeEstudis) {
            PlaEstudis plaEstudis = Parser.plaEstudis(str, this.jornadaLectiva);
            this.plansDeEstudis.afegirPlaEstudis(plaEstudis);
        }
    }

    public void importDataCentreDocent(String centreDocent) {
        if(centreDocent != null) Parser.centreDocent(centreDocent, this);
    }

}

