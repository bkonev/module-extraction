package uk.ac.liv.moduleextraction.axiomdependencies;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import uk.ac.liv.moduleextraction.util.AxiomSplitter;
import uk.ac.liv.moduleextraction.util.FullAxiomComparator;
import uk.ac.liv.moduleextraction.util.ModuleUtils;

import java.util.*;

/**
 * Definitorial depth calculator for ontologies with potentially
 * shared names and repeated axioms. For repeated axioms
 * repeated(A) = A <> C, A <> D is given the max depth of all
 * such axioms depend(A <> X) max(repeated)
 */
public class AxiomDefinitorialDepth {


    private Set<OWLLogicalAxiom> logicalAxioms;
    private HashMap<OWLLogicalAxiom, Integer> axiomDefinitorialDepth;
    private HashMap<OWLClass, Integer> classDefinitorialDepth;
    private HashMap<OWLClass, Set<OWLClass>> immediateDependencies;
    private int max = 0;
    private Set<OWLLogicalAxiom> expressiveAxioms;
    //private OWLDataFactory factory;

    public AxiomDefinitorialDepth(OWLOntology ontology) {
        this(OWLAPIStreamUtils.asSet(ontology.logicalAxioms()));
    }

    public int lookup(OWLLogicalAxiom ax) {
        return axiomDefinitorialDepth.get(ax);
    }

    public AxiomDefinitorialDepth(Set<OWLLogicalAxiom> axioms) {
        //this.factory = OWLManager.getOWLDataFactory();
        this.logicalAxioms = axioms;
        this.axiomDefinitorialDepth = new HashMap<OWLLogicalAxiom, Integer>(axioms.size());
        this.classDefinitorialDepth = new HashMap<OWLClass, Integer>();
        this.immediateDependencies = new HashMap<OWLClass, Set<OWLClass>>();
        this.expressiveAxioms = new HashSet<OWLLogicalAxiom>();
        populateImmediateDependencies();

        generateDefinitorialDepths();
        assignExpressiveAxiomsValue();

    }

    public ArrayList<OWLLogicalAxiom> getDefinitorialSortedList() {
        ArrayList<OWLLogicalAxiom> sortedAxioms = new ArrayList<OWLLogicalAxiom>(logicalAxioms);
        Collections.sort(sortedAxioms, new FullAxiomComparator(axiomDefinitorialDepth));

        return sortedAxioms;
    }

    private void populateImmediateDependencies() {
        for (OWLLogicalAxiom axiom : logicalAxioms) {
            if (ModuleUtils.isInclusionOrEquation(axiom)) {
                OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
                OWLClassExpression definiton = AxiomSplitter.getDefinitionofAxiom(axiom);
                Set<OWLClass> currentDepedencies = immediateDependencies.get(name);
                Set<OWLClass> definitionClasses = OWLAPIStreamUtils.asSet(definiton.classesInSignature());
                //Dependencies don't consider TOP or BOTTOM constructs
//                definitionClasses.remove(factory.getOWLThing());
//                definitionClasses.remove(factory.getOWLNothing());
                if (currentDepedencies == null) {
                    immediateDependencies.put(name, definitionClasses);
                } else {
                    currentDepedencies.addAll(definitionClasses);
                }
            } else {
                expressiveAxioms.add(axiom);
            }
        }

    }

    private void generateDefinitorialDepths() {
        ArrayList<OWLLogicalAxiom> listaxioms = new ArrayList<OWLLogicalAxiom>(logicalAxioms);
        Collections.shuffle(listaxioms);
        for (OWLLogicalAxiom axiom : listaxioms) {
            OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
            axiomDefinitorialDepth.put(axiom, calculateDepth(name));
        }
    }

    /** Names can be recalculated which may be expensive
     *  maybe instead store definitorial depth for concept names again */
    private int calculateDepth(OWLClass name) {
        Integer stored = classDefinitorialDepth.get(name);
        if(stored != null) return stored;

        // else compute the depth
        if (immediateDependencies.get(name) == null) {
            return 0;
        } else {
            HashSet<Integer> depths = new HashSet<Integer>();
            for (OWLClass dep : immediateDependencies.get(name)) {
                if(dep.isOWLNothing() || dep.isOWLThing()){
                    depths.add(0);
                }
                else{
                    depths.add(classDefinitorialDepth.getOrDefault(dep, calculateDepth(dep)));
                }
            }
            int result = 1 + Collections.max(depths);
            max = Math.max(max, result);

            classDefinitorialDepth.put(name, result);
            return result;
        }
    }

    public HashMap<OWLLogicalAxiom, Integer> getAxiomDefinitorialDepthHashMap() {
        return axiomDefinitorialDepth;
    }

    /*
         * Expressive axioms often cannot be realised in terms of definitioral depth
         * (role inclusions) or can create depth cycles (disjointness axioms). So
         * we assign any non-inclusion or equation to be MAX+1 depth of any other
         * axiom in the ontology;
         */
    private void assignExpressiveAxiomsValue() {
        int expressiveValue = max + 1;
        for (OWLLogicalAxiom axiom : expressiveAxioms) {
            axiomDefinitorialDepth.put(axiom, expressiveValue);
        }
    }

}
