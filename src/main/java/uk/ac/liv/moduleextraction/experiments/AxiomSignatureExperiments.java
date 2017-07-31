package uk.ac.liv.moduleextraction.experiments;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import uk.ac.liv.moduleextraction.extractor.NotEquivalentToTerminologyException;
import uk.ac.liv.moduleextraction.signature.SigManager;
import uk.ac.liv.moduleextraction.signature.WriteAxiomSignatures;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.OntologyLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AxiomSignatureExperiments {


    private Experiment experiment;

    public void runExperiments(File signaturesLocation, Experiment experimentType, File resultLocation) throws IOException{
        this.experiment = experimentType;
        System.out.println("Running for: " + signaturesLocation);
        Experiment experiment = experimentType;
        SigManager sigManager = new SigManager(signaturesLocation);
        File[] files = signaturesLocation.listFiles();
        Arrays.sort(files);

		/* Create new folder in result location with same name as signature
		folder */
        File newResultFolder = copyDirectoryStructure(signaturesLocation, "Signatures", resultLocation);
        if(experimentType instanceof ExactlyNDepletingComparison){
            ExactlyNDepletingComparison ndep = (ExactlyNDepletingComparison) experimentType;
            newResultFolder = new File(newResultFolder.getAbsolutePath() + "/" + "domain_size-" + ndep.getDomainSize());
        }


        int experimentCount = 1;
        for(File f : files){
            if(f.isFile()){

                System.out.println("Experiment " + experimentCount + "/" + files.length + ":" + f.getName());
                experimentCount++;
                //New folder in result location - same name as sig file
                File experimentLocation = new File(newResultFolder.getAbsolutePath() + "/" + f.getName());

                if(!experimentLocation.exists()){
                    experimentLocation.mkdirs();
                }

                //If there is already some metrics the experiment is probably finished
                if(experimentLocation.list().length > 0){
                    System.out.println("Experiment results already exists - skipping");
                    continue;
                }

                Set<OWLEntity> sig = sigManager.readFile(f.getName());
                experiment.performExperiment(sig,f);


                //Save the signature with the experiment
                SigManager managerWriter = new SigManager(experimentLocation);
                managerWriter.writeFile(sig, "signature");

                //Write any metrics
                experiment.writeMetrics(experimentLocation);
            }
        }
    }

    /**
     * Copy the structure of a source directory to another location creating a directory
     * for each directory in the path naming the final folder to highlight the experiment
     * @param source - The directory to begin copying from
     * @param sourceLimit - Only start copying the source from this directory
     * @param destination - The top level to copy the structure too
     * @return File - path of deepest part of new directory structure.
     * Example: copyDirectoryStructure(//a/x/y/z/,"x", /home/)
     * result File /home/y/z/
     */
    private File copyDirectoryStructure(File source, String sourceLimit, File destination) {
        Stack<String> directoriesToWrite = new Stack<String>();

        //Push all the directories from the end backwards to the sourceLimit (if applicable)
        while(!source.getName().equals(sourceLimit) && source.getParent() != null){
            if(source.isDirectory()){
                directoriesToWrite.push(source.getName());
            }
            source = source.getParentFile();
        }

        //Build the path from the start of the destinated using the pushed directory names
        String target = destination.getAbsolutePath();
        while(!directoriesToWrite.isEmpty()){
            target = target + "/" + directoriesToWrite.pop();
        }

        File targetFile = new File(target);

        //Name the folder by experiment
        String newFolderName = targetFile.getName() + "-" + experiment.getClass().getSimpleName();
        targetFile = new File(targetFile.getParent() + "/" + newFolderName);

        if(!targetFile.exists()){
            System.out.println("Making directory: " + targetFile.getAbsolutePath());
            targetFile.mkdirs();
        }


        return targetFile;
    }

    public static void main(String[] args) throws OWLOntologyCreationException, NotEquivalentToTerminologyException, IOException, OWLOntologyStorageException, InterruptedException {

        File ontDir = new File(ModulePaths.getOntologyLocation() );
        File[] files = ontDir.listFiles();
        HashMap<String,Integer> ontSize = new HashMap<>();

        for(File ontFile : files) {
            OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ontFile.getAbsolutePath());
            ontSize.put(ontFile.getName(), ont.getLogicalAxiomCount());
            ont.getOWLOntologyManager().removeOntology(ont);
            ont = null;
        }

        Arrays.sort(files, (o1, o2) -> ontSize.get(o1.getName()).compareTo(ontSize.get(o2.getName())));

        System.out.println("Finished sorting ontologies");

        for(File ontFile : files){
            OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ontFile.getAbsolutePath());

            File signatureLocation = new File(ModulePaths.getSignatureLocation() + "/" + ontFile.getName());
            if(!signatureLocation.exists()){
                signatureLocation.mkdirs();
            }

            if(signatureLocation.list().length == 0)
            //Writes every axiom signature of the ontology
            {
                WriteAxiomSignatures axiomSignatures = new WriteAxiomSignatures(ont, signatureLocation);
                axiomSignatures.writeAxiomSignatures();
            }


            int domainSize = 1;
            new AxiomSignatureExperiments().runExperiments(signatureLocation,
                    //new NDepletingExperiment(domainSize,ont,ontFile),
                    new HybridExtractorExperiment(ont,ontFile),
                    new File(ModulePaths.getResultLocation()));

            ont.getOWLOntologyManager().removeOntology(ont);
            ont = null;
        }

    }

}










