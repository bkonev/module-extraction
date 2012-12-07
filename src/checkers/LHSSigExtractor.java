package checkers;



import java.util.HashSet;
import java.util.Set;


import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;

import axioms.AxiomSplitter;

public class LHSSigExtractor {
	private DefinitorialDependencies dependencies;
	private Set<OWLClass> signatureDependencies = new HashSet<OWLClass>();


	public HashSet<OWLLogicalAxiom> getLHSSigAxioms(DefinitorialDependencies deps, Set<OWLLogicalAxiom> ontology, Set<OWLClass> signature){
		this.dependencies = deps;
		HashSet<OWLLogicalAxiom> lhsSigT = new HashSet<OWLLogicalAxiom>();
		generateSignatureDependencies(signature);
		for(OWLLogicalAxiom axiom : ontology){
			OWLClass name = (OWLClass) AxiomSplitter.getNameofAxiom(axiom);
			if(signature.contains(name) || isInSigDependencies(name))
				lhsSigT.add(axiom);
		}
		return lhsSigT;
	}

	private void generateSignatureDependencies(Set<OWLClass> signature) {
		for(OWLClass sigConcept : signature){
			Set<OWLClass> sigDeps = dependencies.getDependenciesFor(sigConcept);
			if(sigDeps != null)
				signatureDependencies.addAll(sigDeps);
		}
	}

	private boolean isInSigDependencies(OWLClass name){
		return signatureDependencies.contains(name);
	}
}
