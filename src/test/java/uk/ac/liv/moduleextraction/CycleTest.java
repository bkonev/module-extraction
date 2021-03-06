package uk.ac.liv.moduleextraction;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.liv.moduleextraction.cycles.OntologyCycleVerifier;
import uk.ac.liv.moduleextraction.util.OntologyLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class CycleTest {

    File dataDirectory;

    @Before
    public void locateFiles(){
        Path resourceDirectory = Paths.get("src/test/data/");
        dataDirectory = resourceDirectory.toFile();
    }


    @Test
    public void simpleCycle(){
        OWLOntology simple = OntologyLoader.loadOntologyAllAxioms(dataDirectory.getAbsolutePath() + "/simplecycle.krss");
        Set<OWLLogicalAxiom> axioms = simple.logicalAxioms().collect(Collectors.toSet());

        /*
        0: D ⊑ A
        1: A ⊑ B
        2: B ⊑ C
        3: C ⊑ D
        */

        OntologyCycleVerifier verifier = new OntologyCycleVerifier(axioms);

        //Ontology is cyclic and all axioms contribute to cycle
        assertTrue(verifier.isCyclic());
        assertEquals(axioms, verifier.getCycleCausingAxioms());

    }

    @Test //Thesis example
    public void complexCycle(){
        OWLOntology complex = OntologyLoader.loadOntologyAllAxioms(dataDirectory.getAbsolutePath() + "/complexcycle.krss");
        List<OWLLogicalAxiom> axioms = complex.logicalAxioms().collect(Collectors.toList());
        //Ensure order is fixed between tests
        Collections.sort(axioms, new AxiomNameComparator());

        /*
        0:A ⊑ B
        1:A ⊑ ∃ r.E
        2:B ⊑ C ⊓ E
        3:C ≡ D ⊓ F
        4:D ⊑ ∀ r.A
        5:E ⊑ G
        6:G ⊑ H ⊓ I
        7:H ⊑ ∃ r.G
        8:W ≡ Z ⊓ (∀ r.W)
        9:X ⊑ Y ⊔ Z
         */


        OntologyCycleVerifier verifier = new OntologyCycleVerifier(axioms);
        //Whole ontology is cyclic
        assertTrue(verifier.isCyclic());

        //Cycle: [B ⊑ C ⊓ E, C ≡ D ⊓ F, D ⊑ ∀ r.A, A ⊑ B] + [A ⊑ ∃ r.E]
        Set<OWLLogicalAxiom> cyclicSubset =
                Stream.of(axioms.get(2), axioms.get(3), axioms.get(4), axioms.get(0), axioms.get(1))
                        .collect(Collectors.toSet());

        verifier = new OntologyCycleVerifier(cyclicSubset);

        assertTrue("Subset is cyclic", verifier.isCyclic());

        Set<OWLLogicalAxiom> cycleCausing = verifier.getCycleCausingAxioms();

        OWLLogicalAxiom unnecessaryAxiom = axioms.get(1);
        //Does not contain [A ⊑ ∃ r.E] even though uses concept name A
        assertTrue("Cycle causing set contains unnecessary axiom: " + unnecessaryAxiom, !cycleCausing.contains(unnecessaryAxiom));

        //Cycle [H ⊑ ∃ r.G, G ⊑ H ⊓ I] + [E ⊑ G]
        cyclicSubset = Stream.of(axioms.get(6), axioms.get(7), axioms.get(5)).collect(Collectors.toSet());


        verifier = new OntologyCycleVerifier(cyclicSubset);

        assertTrue("Subset is cyclic", verifier.isCyclic());

        cycleCausing = verifier.getCycleCausingAxioms();


        assertTrue("Cycle causing set contains unnecessary axiom" + " " + axioms.get(5), !cycleCausing.contains(axioms.get(5)));

        //Single axiom  W ≡ Z ⊓ (∀ r.W)
        cyclicSubset = Collections.singleton(axioms.get(8));

        verifier = new OntologyCycleVerifier(cyclicSubset);

        assertTrue("Subset is cyclic", verifier.isCyclic());
    }

    @Test //Thesis example
    public void removeCycle(){
        OWLOntology complex = OntologyLoader.loadOntologyAllAxioms(dataDirectory.getAbsolutePath() + "/complexcycle.krss");

        List<OWLLogicalAxiom> axioms = complex.logicalAxioms().collect(Collectors.toList());
        OntologyCycleVerifier verifier = new OntologyCycleVerifier(axioms);

        assertTrue("Ontology is cyclic", verifier.isCyclic());

        //Remove those causing a cycle
        axioms.removeAll(verifier.getCycleCausingAxioms());

        //Check the result is acyclic
        verifier = new OntologyCycleVerifier(axioms);

        assertFalse("Ontology should be acyclic",  verifier.isCyclic());
    }

    @Test
    public void selfCyclicCaptured(){
        OWLOntology selfcycle = OntologyLoader.loadOntologyAllAxioms(dataDirectory.getAbsolutePath() + "/selfcycle.krss");
        List<OWLLogicalAxiom> axioms = selfcycle.logicalAxioms().collect(Collectors.toList());
        Collections.sort(axioms, new AxiomNameComparator());


        /*
            0:W ⊑ X
            1:W ⊑ ∃ r.W
        */
        OntologyCycleVerifier verifier = new OntologyCycleVerifier(axioms);

        //Contains self defined axiom so cyclic
        assertTrue("Ontology is cyclic", verifier.isCyclic());

        //Copy of axioms
        HashSet<OWLLogicalAxiom> checkSet = new HashSet<>(axioms);

        //Remove cycle causing axioms should be W ⊑ ∃ r.W
        checkSet.removeAll(verifier.getCycleCausingAxioms());

        //Set still contains W ⊑ X
        assertTrue(checkSet + "should contain " + axioms.get(0), checkSet.contains(axioms.get(0)));

        verifier = new OntologyCycleVerifier(checkSet);

        //Should now be acyclic
        assertFalse("Ontology is acyclic", verifier.isCyclic());

    }

}

