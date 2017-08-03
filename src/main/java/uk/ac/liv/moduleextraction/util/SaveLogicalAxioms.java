package uk.ac.liv.moduleextraction.util;

import org.semanticweb.owlapi.model.*;
import uk.ac.liv.moduleextraction.extractor.NotEquivalentToTerminologyException;

import java.io.File;
import java.io.IOException;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SaveLogicalAxioms {


    public static void main(String[] args) throws OWLOntologyCreationException, NotEquivalentToTerminologyException, IOException, OWLOntologyStorageException, InterruptedException {

        File ontDir = new File(ModulePaths.getOntologyLocation() );
        File[] files = ontDir.listFiles();

        OWLOntology sourceOnt = null;
        OWLOntology targetOnt = null;

        if (files != null) {
            for(File ontFile : files){
                System.out.println(ontFile);
                if(ontFile.getName().contains("_logical")) {
                    System.out.println("skipping");
                    continue;
                }
                try {
                    sourceOnt = OntologyLoader.loadOntologyAllAxioms(ontFile.getAbsolutePath());
                    targetOnt = sourceOnt.getOWLOntologyManager().createOntology(
                            IRI.create(new File(ontFile.getPath() + "_logical")));
                    sourceOnt.logicalAxioms().forEach(targetOnt::add);
                    targetOnt.saveOntology();


                    sourceOnt.getOWLOntologyManager().removeOntology(sourceOnt);
                    targetOnt.getOWLOntologyManager().removeOntology(targetOnt);
                    sourceOnt = null;
                    targetOnt = null;
                }
                catch (UncheckedExecutionException | OWLOntologyStorageException | OWLRuntimeException e) {
                    e.printStackTrace(); // print stack trace and continue
                    if(sourceOnt!=null) {
                        sourceOnt.getOWLOntologyManager().removeOntology(sourceOnt);
                        sourceOnt = null;
                    }
                    if(targetOnt!=null) {
                        targetOnt.getOWLOntologyManager().removeOntology(targetOnt);
                        targetOnt = null;
                    }
                }
            }
        }

    }

}
