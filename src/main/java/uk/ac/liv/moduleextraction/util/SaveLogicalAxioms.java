package uk.ac.liv.moduleextraction.util;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import uk.ac.liv.moduleextraction.extractor.NotEquivalentToTerminologyException;

import java.io.File;
import java.io.IOException;

public class SaveLogicalAxioms {


    public static void main(String[] args) throws OWLOntologyCreationException, NotEquivalentToTerminologyException, IOException, OWLOntologyStorageException, InterruptedException {

        File ontDir = new File(ModulePaths.getOntologyLocation() );
        File[] files = ontDir.listFiles();


        for(File ontFile : files){
            System.out.println(ontFile);
            OWLOntology sourceOnt = OntologyLoader.loadOntologyAllAxioms(ontFile.getAbsolutePath());
            OWLOntology targetOnt = sourceOnt.getOWLOntologyManager().createOntology(
                    IRI.create(new File(ontFile.getPath() + "_logical" )));
            sourceOnt.logicalAxioms().forEach(ax -> targetOnt.add(ax));
            targetOnt.saveOntology();


            sourceOnt.getOWLOntologyManager().removeOntology(sourceOnt);
            targetOnt.getOWLOntologyManager().removeOntology(targetOnt);
            sourceOnt = null;
        }

    }

}
