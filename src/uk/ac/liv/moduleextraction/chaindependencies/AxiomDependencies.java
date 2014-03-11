package uk.ac.liv.moduleextraction.chaindependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import uk.ac.liv.ontologyutils.axioms.AxiomSplitter;
import uk.ac.liv.ontologyutils.axioms.AxiomStructureInspector;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.liv.ontologyutils.util.ModulePaths;
import uk.ac.liv.ontologyutils.util.ModuleUtils;

/***
 * Generates the set of dependT for ontologies 
 * with (possibly) repeated axioms distinishing different
 * dependency sets for each 
 */
public class AxiomDependencies extends HashMap<OWLLogicalAxiom, DependencySet>{

	private AxiomDefinitorialDepth depth;
	private ArrayList<OWLLogicalAxiom> sortedAxioms;
	private AxiomStructureInspector inspector;

	public AxiomDependencies(OWLOntology ontology) {
		this(ontology.getLogicalAxioms());
	}

	public AxiomDependencies(Set<OWLLogicalAxiom> axioms){
		depth = new AxiomDefinitorialDepth(axioms);
		sortedAxioms = depth.getDefinitorialSortedList();
		inspector = new AxiomStructureInspector(axioms);
		calculateDependencies();
	}

	private void calculateDependencies(){
		for(Iterator<OWLLogicalAxiom> it = sortedAxioms.iterator(); it.hasNext();){
			OWLLogicalAxiom nextAxiom = it.next();
			/* No dependencies for non-terminological axioms */
			if(ModuleUtils.isInclusionOrEquation(nextAxiom)){
				updateDependenciesWith(nextAxiom);
			}
		}	
	}

	private void updateDependenciesWith(OWLLogicalAxiom axiom){
		OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
		OWLClassExpression definition = AxiomSplitter.getDefinitionofAxiom(axiom);

		DependencySet axiomDeps = new DependencySet();

		
		addImmediateDependencies(definition,axiomDeps);
		updateFromDefinition(name, definition, axiomDeps);

		put(axiom, axiomDeps);
	}

	private void addImmediateDependencies(OWLClassExpression definition, DependencySet axiomDeps) {
		for(OWLEntity e : definition.getSignature()){
			if(!e.isTopEntity() && !e.isBottomEntity())
				axiomDeps.add(e);
		}
	}

	private void updateFromDefinition(OWLClass axiomName, OWLClassExpression definition, DependencySet axiomDeps) {

		for(OWLClass cls : ModuleUtils.getNamedClassesInSignature(definition)){

			for(OWLLogicalAxiom axiom : getAxiomsWithName(cls)){

				DependencySet clsDependencies = get(axiom);
				if(clsDependencies != null){
					axiomDeps.addAll(clsDependencies);
				}
			}
		}


	}

	private Set<OWLLogicalAxiom> getAxiomsWithName(OWLClass name){
		Set<OWLLogicalAxiom> axioms = new HashSet<OWLLogicalAxiom>();
		axioms.addAll(inspector.getDefinitions(name));
		axioms.addAll(inspector.getPrimitiveDefinitions(name));
		return axioms;
	}

	public ArrayList<OWLLogicalAxiom> getDefinitorialSortedAxioms() {
		return sortedAxioms;
	}

	public static void main(String[] args) {
		//Construct expressive axioms
		OWLDataFactory f = OWLManager.getOWLDataFactory();
		OWLClass a = f.getOWLClass(IRI.create("X#A"));
		OWLClass b = f.getOWLClass(IRI.create("X#B"));
		OWLObjectProperty r = f.getOWLObjectProperty(IRI.create("X#r"));
		OWLObjectProperty s = f.getOWLObjectProperty(IRI.create("X#s"));
		OWLObjectPropertyRangeAxiom range = f.getOWLObjectPropertyRangeAxiom(r, b);
		OWLDisjointClassesAxiom disjoint1 = f.getOWLDisjointClassesAxiom(a,b);
		OWLSubObjectPropertyOfAxiom roleInc = f.getOWLSubObjectPropertyOfAxiom(r, s);
		
		OWLOntology ontology = OntologyLoader.loadOntologyAllAxioms("TestData/dependencies/simple-dependencies.krss");
		
		Set<OWLLogicalAxiom> inputOntology = ontology.getLogicalAxioms();
		inputOntology.add(disjoint1);
		inputOntology.add(range);
		inputOntology.add(roleInc);
		
		AxiomDefinitorialDepth d = new AxiomDefinitorialDepth(inputOntology);

		for(OWLLogicalAxiom ax : d.getDefinitorialSortedList()){
			System.out.println(d.lookup(ax) + ":" + ax);
		}
			
		AxiomDependencies depend = new AxiomDependencies(inputOntology);
		System.out.println(depend);
	}
}
