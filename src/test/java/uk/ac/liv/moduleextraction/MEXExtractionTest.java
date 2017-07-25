package uk.ac.liv.moduleextraction;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import uk.ac.liv.moduleextraction.extractor.ExtractorException;
import uk.ac.liv.moduleextraction.extractor.MEX;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.moduleextraction.util.OntologyLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;


/**
 * Created by william on 28/01/17.
 */
public class MEXExtractionTest {


    File dataDirectory;

    @Before
    public void locateFiles(){
        Path resourceDirectory = Paths.get("src/test/data/");
        dataDirectory = resourceDirectory.toFile();
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @Test
    public void simpleIndirectDependencyExtraction() throws ExtractorException {
        OWLOntology equiv = OntologyLoader.loadOntologyAllAxioms(dataDirectory.getAbsolutePath() + "/equiv.krss");

        ModuleUtils.remapIRIs(equiv, "X");
        List<OWLLogicalAxiom> axioms = equiv.logicalAxioms().collect(Collectors.toList());
        Collections.sort(axioms, new AxiomNameComparator());

        /*
        0:A ≡ B1 ⊓ B2
        1:A1 ⊑ B1
        2:A2 ⊑ B2
        3:C ⊑ A2
        */

        OWLDataFactory f = equiv.getOWLOntologyManager().getOWLDataFactory();
        OWLClass a = f.getOWLClass(IRI.create("X#A"));
        OWLClass a1 = f.getOWLClass(IRI.create("X#A1"));
        OWLClass a2 = f.getOWLClass(IRI.create("X#A2"));

        //  Σ = [A1, A, A2]
        Set<OWLEntity> sig = new HashSet<>(Arrays.asList(a, a1, a2));

        MEX mex = new MEX(axioms.stream().collect(Collectors.toSet()));

        //Module  {A ≡ B1 ⊓ B2, A1 ⊑ B1, A2 ⊑ B2}
        Set<OWLLogicalAxiom> expectedMexModule = Stream.of(axioms.get(0), axioms.get(1), axioms.get(2)).collect(Collectors.toSet());
        Set<OWLLogicalAxiom> mexModule = mex.extractModule(sig);


        assertEquals(expectedMexModule, mexModule);
    }
}
