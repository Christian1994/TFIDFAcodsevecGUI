/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logica;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import mersennetwister.MersenneTwisterFast;
import modelo.Dataset;
import modelo.Diagnostico;

/**
 *
 * @author crisd
 */
public class DatasetLogica {
    
    Dataset ds = new Dataset();
    Map<Long, Diagnostico> hcs = ds.getHistoriasClinicas();
    ArrayList<String> diseasesHC = ds.getEnfermedadesHC();
    ArrayList<String> diseasesSet = ds.getConjEnfermedades();
    ArrayList<String> symptomsHC = ds.getSintomasHC();
    ArrayList<String> symptomsSet = ds.getConjSintomas();
    
    Map<Long, Diagnostico> trainingHC = new HashMap<>();
    Map<Long, Diagnostico> testingHC = new HashMap<>();
        
    private int [][] matrizTF;                      // Matriz TF (Frecuencia de término) que a su vez es la matriz de la cantidad de HC's de 
                                                    // una enfermedad que contiene el síntoma, pues el síntoma se escribe una sóla vez por cada HC.
    private double [][] matrizIDF;                  // Matriz IDF (Frencuencia inversa de documento)
    private int [] cantidadHCSintoma;               // Arreglo de cantidad de HC's que contengan el síntoma
    private double [][] matrizTFIDF;                // Matriz TF*IDF
    
    private double [][] matrizNormalizada;
    private int [] cadenaPrueba;
    
    private double [] cadenaSimilaridad;
    private double [] cadenaSimilaridadOrdDesc;

    private int cantidadAciertos = 0;
    private int cantidadDesaciertos = 0;
    private double porcentajePrecision = 0.0;
    
    public DatasetLogica(){

    }

    public double getPorcentajePrecision() {
        return porcentajePrecision;
    }

    public void setPorcentajePrecision(double porcentajePrecision) {
        this.porcentajePrecision = porcentajePrecision;
    }
    
    // Impresión de datos de cada Diagnóstico    
    public void imprimirDiagnosticos(JTextArea areaResultados){
        for (long key : hcs.keySet()) {
            areaResultados.append(hcs.get(key).getReferencia() + " | ");
            areaResultados.append(hcs.get(key).getEnfermedad() + " | ");
            areaResultados.append(Arrays.toString(hcs.get(key).getSintomas()));
            areaResultados.append("\n");
        }      
    }
        
    // Impresión de todas las enfermedades sin repetir
    public void generarConjuntoEnfermedades(JTextArea areaResultados){
        // Agregación de enfermedades
        areaResultados.append("\n");
        for (long key : hcs.keySet()) {
            diseasesHC.add(hcs.get(key).getEnfermedad());
        }

        // Asignación para almacenar los elementos de enfermedades sin repetir
        HashSet<String> diseaseSet = new HashSet<>(diseasesHC);
        diseasesSet.addAll(diseaseSet);

        // Impresión de las enfermedades sin repetir
        areaResultados.append("Enfermedades: \n");
        areaResultados.append("\n");
        for(String disease : diseasesSet){
            areaResultados.append(disease + "\n");
        }
        areaResultados.append("\n");
    }
    
    // Impresión de todos los síntomas sin repetir
    public void generarConjuntoSintomas(JTextArea areaResultados){
        // Agregación de síntomas
        for (long key : hcs.keySet()) {
            String [] conjuntoSintomas = hcs.get(key).getSintomas();         
            symptomsHC.addAll(Arrays.asList(conjuntoSintomas));
        }

        // Asignación para almacenamiento de elementos de síntomas sin repetir
        HashSet<String> symptomSet = new HashSet<>(symptomsHC);
        symptomsSet.addAll(symptomSet);

        // Impresión de los síntomas sin repetir
        areaResultados.append("Síntomas: \n");
        areaResultados.append("\n");
        for(String symptoms : symptomsSet){
            areaResultados.append(symptoms + "\n");
        }
        areaResultados.append("\n");
    }    
    
    // Entrena al sistema mediante el algoritmo de clustering TF-IDF
    public void entrenamiento(JTextArea areaResultados){
        
        this.seleccionAleatoria();
        // this.imprimirDiagnosticosSeleccionados(areaResultados);
        
        matrizTF = new int [diseasesSet.size()][symptomsSet.size()];
        matrizIDF = new double [diseasesSet.size()][symptomsSet.size()];
        matrizTFIDF = new double [diseasesSet.size()][symptomsSet.size()];
        cantidadHCSintoma = new int [symptomsSet.size()];

        // Genera el arreglo de la cantidad de HC's que contiene el síntoma
        for(int i = 0; i < symptomsSet.size(); i++){
            String sintoma = symptomsSet.get(i);                
            for(long key: trainingHC.keySet()){
                String [] symptomSet = trainingHC.get(key).getSintomas();
                for(String symptom : symptomSet){
                    if(sintoma.equals(symptom)){
                        cantidadHCSintoma[i]++;
                    }
                }                
                /*if(enfermedad.equals(trainingHC.get(key).getEnfermedad())){
                    cantidadHCEnfermedad[i]++;
                }*/
            }                            
        }

        // Imprime el arreglo de la cantidad de HC's por síntoma
        for(int i = 0; i < cantidadHCSintoma.length; i++){
            areaResultados.append(symptomsSet.get(i) + ": " + cantidadHCSintoma[i] + "\n");
        }
        areaResultados.append("\n");
        areaResultados.append("\n");
        
        this.generarMatrizTF(matrizTF);                 // Genera la matriz TF        
        this.generarMatrizIDF(matrizIDF);                // Genera la matriz IDF
        this.generarMatrizTFIDF(matrizTFIDF);              // Genera la matriz TF*IDF        
        
        // Imprime la matriz TF
        DecimalFormat formateadorTF = new DecimalFormat("000");
        areaResultados.append("Matriz TF (Term Frequency): \n\n");
        for(int i = 0; i < matrizTF.length; i++){
            for(int j = 0; j < matrizTF[i].length; j++){
                areaResultados.append(formateadorTF.format(matrizTF[i][j]) + " ");
            }
            areaResultados.append("\n");
        }
        areaResultados.append("\n");

        // Imprime la matriz IDF
        DecimalFormat formateador = new DecimalFormat("0.0000");
        areaResultados.append("Matriz IDF (Inverse Document Frequency): \n\n");        
        for(int i = 0; i < matrizIDF.length; i++){
            for(int j = 0; j < matrizIDF[i].length; j++){
                areaResultados.append(formateador.format(matrizIDF[i][j]) + " ");
                // areaResultados.append(matrizIDF[i][j] + " ");
            }
            areaResultados.append("\n");
        }
        areaResultados.append("\n");

        // Imprime la matriz TFIDF
        areaResultados.append("Matriz TF-IDF (TF * IDF): \n\n");        
        for(int i = 0; i < matrizTFIDF.length; i++){
            for(int j = 0; j < matrizTFIDF[i].length; j++){
                areaResultados.append(formateador.format(matrizTFIDF[i][j]) + " ");
                // areaResultados.append(matrizIDF[i][j] + " ");
            }
            areaResultados.append("\n");
        }
        areaResultados.append("\n");        
        
        // Normaliza la tabla
        this.normalizacion(matrizTFIDF);
        
        // Imprime la matriz de conocimiento normalizada entrenada para aplicar las técnicas de IA
        areaResultados.append("Matriz Normalizada: \n\n");        
        for(int i = 0; i < matrizNormalizada.length; i++){
            for(int j = 0; j < matrizNormalizada[i].length; j++){
                areaResultados.append(formateador.format(matrizNormalizada[i][j]) + " ");
            }
            areaResultados.append("\n");
        }
        areaResultados.append("\n");        
    }
            
    // Prueba el sistema
    public void pruebas(JTextArea areaResultados){
        
        this.crearConjuntoPrueba();
        this.imprimirDiagnosticosPrueba(areaResultados);
        
        for(long key: testingHC.keySet()){
            cadenaPrueba = new int [symptomsSet.size()];
            cadenaSimilaridad = new double [diseasesSet.size()];
            
            String [] symptomSet = testingHC.get(key).getSintomas();

            // Crea la cadena de prueba
            this.crearCadenaPrueba(cadenaPrueba, symptomSet, areaResultados);

            // Medida de similaridad de HC de prueba con cada enfermedad (clase)
            for(int i = 0; i < cadenaSimilaridad.length; i++){
                cadenaSimilaridad[i] = this.similaridad(matrizNormalizada[i], cadenaPrueba);
                areaResultados.append(diseasesSet.get(i) + ": " + cadenaSimilaridad[i] + "\n");
            }

            // Mayor similaridad determina el diagnóstico que arroje el sistema
            int indice = 0;
            double mayor = cadenaSimilaridad[indice];
            for(int i = 0; i < cadenaSimilaridad.length; i++){
                if(cadenaSimilaridad[i] > mayor){
                    mayor = cadenaSimilaridad[i];
                    indice = i;
                }
            }

            areaResultados.append("\n");
            areaResultados.append("Diagnóstico: " + diseasesSet.get(indice) + "\n");
            areaResultados.append("Mayor similaridad: " + mayor + "\n");
            areaResultados.append("Enfermedad esperada: " + testingHC.get(key).getEnfermedad() + " -> Enfermedad diagnosticada: " + diseasesSet.get(indice) + "\n");
            areaResultados.append("\n");                      

            if(testingHC.get(key).getEnfermedad().equals(diseasesSet.get(indice))){
                cantidadAciertos++;
            }
            else{
                cantidadDesaciertos++;
            }
        }
    }
    
    // Prueba con el Veterinario Experto
    public void pruebaVet(int[] testArray){
        
        int cantidadSintomasSeleccionados = 0;
        
        // Validar cadena de prueba para que el sistema arroje un diagnóstico
        for(int i = 0; i < testArray.length; i++){
            if(testArray[i] == 1)
                cantidadSintomasSeleccionados++;
        }
        
        if(cantidadSintomasSeleccionados >= 2){
            cadenaSimilaridad = new double [diseasesSet.size()];        

            // Medida de similaridad de HC de prueba con cada enfermedad (clase)
            for(int i = 0; i < cadenaSimilaridad.length; i++){
                cadenaSimilaridad[i] = this.similaridad(matrizNormalizada[i], testArray);
                System.out.print(diseasesSet.get(i) + ": " + cadenaSimilaridad[i] + "\n");
            }

            // Mayor similaridad determina el diagnóstico que arroje el sistema
            int indice = 0;
            double mayor = cadenaSimilaridad[indice];
            for(int i = 0; i < cadenaSimilaridad.length; i++){
                if(cadenaSimilaridad[i] > mayor){
                    mayor = cadenaSimilaridad[i];
                    indice = i;
                }
            }

            // Mensaje de Diagnóstico Presuntivo. Win!!!
            JOptionPane.showMessageDialog(null, "El canino puede tener: " + diseasesSet.get(indice), "Diagnóstico.", JOptionPane.INFORMATION_MESSAGE);

            System.out.print("\n");
            System.out.print("Diagnóstico: " + diseasesSet.get(indice) + "\n");
            System.out.print("Mayor similaridad: " + mayor + "\n");
            System.out.print("Enfermedad diagnosticada: " + diseasesSet.get(indice) + "\n");
            System.out.print("\n");            
        }
        else{
            // Debes seleccionar al menos dos síntomas para diagnosticar la enfermedad
            JOptionPane.showMessageDialog(null, "Debes seleccionar al menos dos síntomas para diagnosticar la enfermedad.", "Error", JOptionPane.ERROR_MESSAGE);            
        }    
    }
    
    // Muestra las estadísticas
    public void estadisticas(JTextArea areaResultados){
        DecimalFormat formateador = new DecimalFormat("0.00");
        porcentajePrecision = ((double)cantidadAciertos / testingHC.size()) * 100;

        areaResultados.append("Cantidad de Historias clínicas: " + hcs.size() + "\n");
        areaResultados.append("Cantidad de Enfermedades: " + diseasesSet.size() + "\n");
        areaResultados.append("Cantidad de Síntomas: " + symptomsSet.size() + "\n");
        areaResultados.append("\n");
        areaResultados.append("Cantidad de Historias clínicas seleccionadas para entrenar: " + trainingHC.size() + "\n");
        areaResultados.append("Cantidad de Historias clínicas seleccionadas para probar: " + testingHC.size() + "\n");
        areaResultados.append("\n");
        areaResultados.append("Cantidad de aciertos: " + cantidadAciertos + "\n");
        areaResultados.append("Cantidad de desaciertos: " + cantidadDesaciertos + "\n");
        areaResultados.append("Procentaje de precisión: " + formateador.format(porcentajePrecision) + "%\n");
        areaResultados.append("\n");
    }    
    
//----------------------- Funciones auxiliares para las operaciones del Dataset --------------------------------

    // Selecciona aleatoriamente las historias clínicas para entrenamiento
    public void seleccionAleatoria(){
        int i = 0;
        MersenneTwisterFast mt = new MersenneTwisterFast();                 // Generador de números pseudoaleatorios MersenneTwister
        mt.setSeed((long)(hcs.size() * Math.random() + 1));   // Semilla     
        do{           
            long claveAux = (long)(mt.nextDouble() * hcs.size() + 1); 
            // areaResultados.append(claveAux);            
            // long claveAux = (long)(hcs.size() * Math.random() + 1);
            if(!hcs.get(claveAux).isSeleccionado()){
                trainingHC.put(claveAux, hcs.get(claveAux));
                trainingHC.get(claveAux).setSeleccionado(true);
                i++;
                mt.setSeed((long)(hcs.size() * Math.random() + 1));   // Semilla 
            }
        }
        while(i < hcs.size() * 0.8);
    }
    
    // Impresión de historias clínicas seleccionadas aleatoriamente    
    /* public void imprimirDiagnosticosSeleccionados(JTextArea areaResultados){
        for (long key : trainingHC.keySet()) {
            areaResultados.append(trainingHC.get(key).getReferencia() + " | ");
            areaResultados.append(trainingHC.get(key).getEnfermedad() + " | ");
            areaResultados.append(Arrays.toString(trainingHC.get(key).getSintomas()));
            areaResultados.append("\n");
        }
        areaResultados.append("\n");
    }*/    

    // Genera la matriz TF
    public void generarMatrizTF(int [][] TFMatrix){
        for(int i = 0; i < diseasesSet.size(); i++){
            String enfermedad = diseasesSet.get(i);
            
            for(int j = 0; j < symptomsSet.size(); j++){
                String sintoma = symptomsSet.get(j);
                
                for(long key: trainingHC.keySet()){
                    if(enfermedad.equals(trainingHC.get(key).getEnfermedad())){
                        String [] symptomSet = trainingHC.get(key).getSintomas();
                        for(String symptom : symptomSet){
                            if(sintoma.equals(symptom)){
                                TFMatrix[i][j]++;
                            }
                        }
                    }
                }
                
            }            
        }        
    }
    
    // Genera la matriz IDF -> IDF = log((cantidadHC)/(1 + cantidadHCsintoma))
    public void generarMatrizIDF(double [][] IDFMatrix){
        for(int i = 0; i < diseasesSet.size(); i++){
            String enfermedad = diseasesSet.get(i);

            for(int j = 0; j < symptomsSet.size(); j++){
                String sintoma = symptomsSet.get(j);

                for(long key: trainingHC.keySet()){
                    if(enfermedad.equals(trainingHC.get(key).getEnfermedad())){
                        String [] symptomSet = trainingHC.get(key).getSintomas();
                        for(String symptom : symptomSet){
                            if(sintoma.equals(symptom)){
                                IDFMatrix[i][j] = Math.log(trainingHC.size() / (double)(1 + cantidadHCSintoma[j]));
                            }
                        }
                    }
                }                
            }            
        }            
    }    

    // Genera la matriz TF-IDF
    public void generarMatrizTFIDF(double [][] TFIDFMatrix){
        for(int i = 0; i < diseasesSet.size(); i++){            
            for(int j = 0; j < symptomsSet.size(); j++){                
                TFIDFMatrix[i][j] = matrizTF[i][j] * matrizIDF[i][j];                
            }            
        }        
    }    
    
    // Función auxiliar para normalizar la tabla (Valor elemento sobre la norma vectorial)
    public double [][] normalizacion(double [][] tabla){
        matrizNormalizada = new double[diseasesSet.size()][symptomsSet.size()];
        for(int i = 0; i < tabla.length; i++){
            for(int j = 0; j < tabla[i].length; j++){
                matrizNormalizada[i][j] = tabla[i][j] / this.normaVectorial(tabla[i]);
            }
        }
        
        return matrizNormalizada;
    }
    
    // Función auxiliar para calcular la norma vectorial
    public double normaVectorial(double [] arreglo){
        double norma = 0.0;
        
        for(int i = 0; i < arreglo.length; i++){
            norma += Math.pow(arreglo[i], 2);
        }
        
        return Math.pow(norma, 0.5);
    }

    // Selecciona el conjunto de HC's de prueba
    public void crearConjuntoPrueba(){
        for(long key : hcs.keySet()){
            if(!hcs.get(key).isSeleccionado()){
                testingHC.put(key, hcs.get(key));
                testingHC.get(key).setSeleccionado(true);
            }            
        }        
    }

    // Impresión de historias clínicas seleccionadas aleatoriamente    
    public void imprimirDiagnosticosPrueba(JTextArea areaResultados){
        areaResultados.append("Diagnósticos de Prueba");
        areaResultados.append("\n");
        areaResultados.append("\n");
        for (long key : testingHC.keySet()) {
            areaResultados.append(testingHC.get(key).getReferencia() + " | ");
            areaResultados.append(testingHC.get(key).getEnfermedad() + " | ");
            areaResultados.append(Arrays.toString(testingHC.get(key).getSintomas()));
            areaResultados.append("\n");
        }
        areaResultados.append("\n");
    }    

    // Crea la cadena de prueba (Rrepresentación de HC de prueba)
    public void crearCadenaPrueba(int [] testString, String [] symptomSet, JTextArea areaResultados){
        for(String symptom : symptomSet){
            for(int i = 0; i < symptomsSet.size(); i++){
                String sintoma = symptomsSet.get(i);
                if(sintoma.equals(symptom)){
                    cadenaPrueba[i] = 1;
                }
            }
        }

        for(int i = 0; i < cadenaPrueba.length; i++){
            areaResultados.append(cadenaPrueba[i] + " ");
        }
        areaResultados.append("\n");
        areaResultados.append("\n");

        for(int i = 0; i < cadenaPrueba.length; i++){
            if(cadenaPrueba[i] == 1){
                areaResultados.append(symptomsSet.get(i) + "\n");
            }
        }
        areaResultados.append("\n");
               
    }
    
    // Función auxiliar para calcular la medida de similaridad
    public double similaridad(double [] normCadena, int [] testCadena){
        double valorSimilaridad = 0.0;
        
        for(int i = 0; i < normCadena.length; i++){
            valorSimilaridad += normCadena[i] * testCadena[i];
        }
        
        return valorSimilaridad;
    }
    
    // Función auxiliar para retornar el mayor valor de similaridad
    /* public double mayorSimilaridad(double [] similarCadena){
        int indice = 0;
        double mayor = cadenaSimilaridad[indice];
        for(int i = 0; i < cadenaSimilaridad.length; i++){
            if(cadenaSimilaridad[i] > mayor){
                mayor = cadenaSimilaridad[i];
                indice = i;
            }          
        }
        areaResultados.append("Diagnóstico: " + diseasesSet.get(indice));        
        return mayor;
    }*/    
    
}
