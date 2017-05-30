package uk.ac.liv.moduleextraction.examples;

import org.semanticweb.owlapi.model.*;
import uk.ac.liv.moduleextraction.experiments.AMEXvsSTAR;
import uk.ac.liv.moduleextraction.experiments.Experiment;
import uk.ac.liv.moduleextraction.experiments.MultipleExperiments;
import uk.ac.liv.moduleextraction.experiments.NDepletingExperiment;
import uk.ac.liv.moduleextraction.extractor.ExtractorException;
import uk.ac.liv.moduleextraction.extractor.STARAMEXHybridExtractor;
import uk.ac.liv.moduleextraction.extractor.STARExtractor;
import uk.ac.liv.moduleextraction.extractor.STARMEXHybridExtractor;
import uk.ac.liv.moduleextraction.signature.SigManager;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.signature.WriteAxiomSignatures;
import uk.ac.liv.moduleextraction.signature.WriteRandomSigs;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.moduleextraction.util.OntologyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by william on 18/04/17.
 */
public class ModuleExtractionExamples {

    public static final String ONTOLOGY_OWL = "test/data/galen.owl"; // ontology file
    //public static final String ONTOLOGY_OWL = "/Users/konev/Downloads/ATC.ttl"; // ontology file
    //public static final String ONTOLOGY_OWL = "test/data/equiv.krss"; // ontology file
    //public static final String ONTOLOGY_OWL = "/Users/konev/Downloads/NCITNCBO.owl"; // ontology file
    //public static final String ONTOLOGY_OWL = "/Users/konev/work/Liverpool/anti/software/GFO/gfo-basic.owl"; // ontology file
    public static final String PATH_TO_SIGNATURES = "/tmp"; // path to signatures
    public static final String PATH_TO_RESULTS = "/tmp"; // path to results


    public static void main(String[] args) {
        // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
        /* Run the examples */
        try {
            ModuleExtractionExamples.usingModuleExtractors();
        } catch (ExtractorException e) {
            e.printStackTrace();
        }
        //ModuleExtractionExamples.generatingSignatures();
        //ModuleExtractionExamples.writeSignaturesToFile();
        //etc...

    }

    public static void usingModuleExtractors() throws ExtractorException {
        //Load the ontology from a file - CHANGE THIS to your own ontology
        //Make sure ontologies are suitable for use with the starExtractor before using it - i.e don't use more expressive  than ALCQI with AMEX
        Logger logger = LoggerFactory.getLogger(ModuleExtractionExamples.class);

        System.out.print("Loading ontology...");
        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ONTOLOGY_OWL);
        System.out.println("done");

        //Create the module extractors - implement the Extractor interface
        //AMEX amex = new AMEX(ont);
        STARExtractor           starExtractor            = new STARExtractor(ont);
        STARAMEXHybridExtractor staramexHybridExtractor  = new STARAMEXHybridExtractor(ont);
        STARMEXHybridExtractor  starmexHybridExtractor   = new STARMEXHybridExtractor(ont);
        //MEX mex = new MEX(ont);

        //Generate a set of 1000 random axioms from the ontology
        System.out.print("Generating signatures...");
        Set<OWLLogicalAxiom> randomAxs = ModuleUtils.generateRandomAxioms(ont.getLogicalAxioms(),1000);
        System.out.println("done");

        System.out.println("Starting module extraction ...");
        System.out.println("STAR-AMEX, STAR-MEX, STAR");

        //Extract a module for the signature of each axiom
        for(OWLLogicalAxiom ax : randomAxs){

            //Signature of the axiom
            Set<OWLEntity> sig = ax.getSignature();

            logger.trace("Signature: {}", sig);

            //Extract the modules - N.B don't need a new starExtractor object for each signature
            //Set<OWLLogicalAxiom> amexMod = amex.extractModule(sig);
            Set<OWLLogicalAxiom> starMod     = starExtractor.extractModule(sig);
            Set<OWLLogicalAxiom> starmexMod  = starmexHybridExtractor.extractModule(sig);
            Set<OWLLogicalAxiom> starAmexMod = staramexHybridExtractor.extractModule(sig);

            logger.trace("eli hybrid module {}", starmexMod);
            logger.trace("alcqui hybrid module {}", starAmexMod);
            logger.trace("star module {}", starMod);

            //System.out.println("AMEX: " + amexMod.size());

            System.out.print(starAmexMod.size());
            System.out.print(", " + starmexMod.size());
            System.out.print(", " + starMod.size());
            System.out.println();
        }
        System.out.println("...done");

    }

    public static void generatingSignatures(){
        //Load ontology
        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ONTOLOGY_OWL);

        //Intitalise the signature generator
        SignatureGenerator gen = new SignatureGenerator(ont.getLogicalAxioms());

        //Random signature of 100 symbols (roles or concepts)
        Set<OWLEntity> sig = gen.generateRandomSignature(100);

        //Random signature of 100 concept names
        Set<OWLClass> sigCls = gen.generateRandomClassSignature(100);

        //Random signature of 100 role names
        Set<OWLObjectProperty> roleSig = gen.generateRandomRoles(100);

        //Extract modules with signatures etc...
    }



    public static void writeSignaturesToFile(){
        /* Writing signatures to file useful for experiments and reproducing */

        //Ontology
        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ONTOLOGY_OWL);

        //Location to save signatures
        File f = new File("/path/to/Signatures");

        //Writes every axiom signature of the ontology to the given location
        WriteAxiomSignatures axiomSignatures = new WriteAxiomSignatures(ont, f);
        axiomSignatures.writeAxiomSignatures();

        /* Completely random */
        WriteRandomSigs random = new WriteRandomSigs(ont, f);
        //Write 1000 signatures of 50 random symbols to the location
        random.writeSignature(50, 1000);
        //Write 500 signatures of 100 random symbols to the location
        random.writeSignature(100, 500);

        /* Concepts + role percentage */
        //Write 1000 signatures consisting of 50 random concept names + 25% of all roles taken randomly from the ontology
        random.writeSignatureWithRoles(50, 25, 1000);

        //Write 1000 signatures consisting of 100 random concept names 0% of all roles (purely concept signatures)
        random.writeSignatureWithRoles(100, 0, 1000);
    }


    public static void readSignaturesFromFile() throws IOException {
        //Set up signature manager pointing to a chosen directory
        SigManager manager = new SigManager(new File(PATH_TO_SIGNATURES));

        //Read signature from file in chosen directory
        Set<OWLEntity> sig = manager.readFile("signature.txt");

        //Modify signature perhaps..

        //Write signature to file in same chosen directory
        manager.writeFile(sig, "signature-modified.txt");
     }


    public void runningExperiments() {
        //Load the ontology
        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ONTOLOGY_OWL);

        //These are for running things and saving the results
        //All experiments use the Experiment interface

        //Construct experiment with ontology
        Experiment e = new AMEXvsSTAR(ont);


        //Generate signatures
        SignatureGenerator gen = new SignatureGenerator(ont.getLogicalAxioms());

        //Where to save the results - multiple results can be saved to the same directory
        File resultLocation = new File(PATH_TO_RESULTS);

        for (int i = 0; i < 100; i++) {
            Set<OWLEntity> signature = gen.generateRandomSignature(100);
            //Run the experiment with given signature and save in result location
            e.performExperiment(signature, resultLocation);
        }
    }

    public void multipleExperiments() throws IOException {
        //Ont file
        File ontLocation = new File(ONTOLOGY_OWL);

        //Load the ontology from the file
        OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ontLocation.getAbsolutePath());

        int domain_size = 2;

        //2-depleting experiment, takes ont location as a param as it's included in the output
        Experiment ndepletingExperiment = new NDepletingExperiment(domain_size, ont, ontLocation);

        //Multiple experiments at once - utility for ease of use
        MultipleExperiments multiple = new MultipleExperiments();


        File signatureLocation = new File(PATH_TO_SIGNATURES);
        File resultLocation = new File(PATH_TO_RESULTS);

        //Writes every axiom signature of the ontology
        WriteAxiomSignatures axiomSignatures = new WriteAxiomSignatures(ont, signatureLocation);
        axiomSignatures.writeAxiomSignatures();

        //For every signature in the signatureLocation directory, run the experiment, and save the result to the resultLocation directory
        multiple.runExperiments(signatureLocation, ndepletingExperiment, resultLocation);

    }


}
