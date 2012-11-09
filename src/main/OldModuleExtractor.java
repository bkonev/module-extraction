package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import loader.OntologyLoader;


import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import qbf.QBFSolver;
import qbf.QBFSolverException;

import checkers.ALCAxiomChecker;
import checkers.ELChecker;
import checkers.InseperableChecker;
import checkers.LHSSigExtractor;
import checkers.SyntacticDependencyChecker;

import util.ModuleUtils;

public class OldModuleExtractor {

	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	SyntacticDependencyChecker syntaxDepChecker = new SyntacticDependencyChecker();
	InseperableChecker insepChecker = new InseperableChecker();
	QBFSolver qbfSolver = new QBFSolver();
	LHSSigExtractor lhsExtractor = new LHSSigExtractor();
	int maxPercentageComplete = 0;

	public OldModuleExtractor() throws OWLOntologyCreationException {
	
	}
	
	public HashSet<OWLLogicalAxiom> extractModule(OWLOntology ontology, Set<OWLClass> signature) throws IOException, QBFSolverException{
		
		HashSet<OWLLogicalAxiom> module = new HashSet<OWLLogicalAxiom>();
		HashSet<OWLLogicalAxiom> W = new HashSet<OWLLogicalAxiom>();
		HashSet<OWLLogicalAxiom> terminology = (HashSet<OWLLogicalAxiom>) ontology.getLogicalAxioms();
		
		int iterations = 1;
		
		while(!hasTheSameAxioms(getDifference(terminology, module),W)){		
			HashSet<OWLLogicalAxiom> differenceOfAll = getDifference(getDifference(terminology, module),W);
			OWLLogicalAxiom a = chooseAxiomOfSet(differenceOfAll);
			//System.out.println("Chosen axiom: " + a);

			W.add(a);
			
			Set<OWLClass> signatureAndM = new HashSet<OWLClass>();
			signatureAndM.addAll(signature);
			signatureAndM.addAll(ModuleUtils.getClassesInSet(module));
			printPercentageComplete(W, terminology, module);
			
			HashSet<OWLLogicalAxiom> lhsSigT = lhsExtractor.getLHSSigAxioms(W, signatureAndM);
			
			if(syntaxDepChecker.hasSyntacticSigDependency(W, signatureAndM) 
					|| insepChecker.isSeperableFromEmptySet(lhsSigT, signatureAndM)){

				module.add(a);
				W.clear();
			}
			
			iterations++;
		}

		System.out.format("\n100%% complete - Complete in %d iterations\n", iterations);
		return module;
	}

	public HashSet<OWLLogicalAxiom> getDifference(HashSet<OWLLogicalAxiom> ontology1, HashSet<OWLLogicalAxiom> ontology2){
		@SuppressWarnings("unchecked")
		HashSet<OWLLogicalAxiom> temp = (HashSet<OWLLogicalAxiom>) ontology1.clone();
		temp.removeAll(ontology2);
		return temp;
	}
		
	@SuppressWarnings("unused")
	private void printPercentageComplete(Set<OWLLogicalAxiom> W, Set<OWLLogicalAxiom> terminology, Set<OWLLogicalAxiom> module) {
		int terminologySize = terminology.size();
		int wSize = W.size();

		int percentage = (int) Math.floor(((double)wSize/terminologySize)*100);
		if(percentage > maxPercentageComplete){
			maxPercentageComplete = percentage;
			System.out.print((maxPercentageComplete % 10 == 0)? maxPercentageComplete + "% complete \n" : "");
		}
		System.out.println(wSize + ":" + module.size());
	}


	
	private boolean hasTheSameAxioms(Set<OWLLogicalAxiom> ontology1, Set<OWLLogicalAxiom> ontology2){
		return ontology1.equals(ontology2);
	}

	private OWLLogicalAxiom chooseAxiomOfSet(HashSet<OWLLogicalAxiom> ont){
		@SuppressWarnings("unchecked")
		HashSet<OWLLogicalAxiom> axs = (HashSet<OWLLogicalAxiom>) ont.clone();
		//TreeSet<OWLLogicalAxiom> axioms = new TreeSet<OWLLogicalAxiom>(axs);
		
		ArrayList<OWLLogicalAxiom> listy = new ArrayList<OWLLogicalAxiom>(axs);
		
		return listy.get(0);
	}
	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		OWLDataFactory f = OWLManager.getOWLDataFactory();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	
		OWLOntology ont = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/module/pathway.obo");
		
		OWLOntology ont2 = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/module/material.owl");

		OWLOntology one = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/interp/diff.krss");
		OWLOntology two = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/interp/diff2.krss");
		
		OWLOntology nci1 = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/NCI/nci-09.03d.owl");
		//OWLOntology nci2 = OntologyLoader.loadOntology("/users/loco/wgatens/Ontologies/NCI/nci-10.02d.owl");

		OldModuleExtractor mod = null;
		try {
			mod = new OldModuleExtractor();
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}
		int percentOfAxioms = (int) Math.max(1, Math.round(((double) ont.getLogicalAxiomCount()/100)*10));
		
		
		OWLOntology chosenOnt = nci1;
		
		HashSet<OWLLogicalAxiom> alcAxioms = new HashSet<OWLLogicalAxiom>();
		for(OWLLogicalAxiom axiom : chosenOnt.getLogicalAxioms()){
			if(new ALCAxiomChecker().isALCAxiom(axiom) && !(new ELChecker().isELAxiom(axiom))){
				alcAxioms.add(axiom);
			}
		}
		
		Set<OWLClass> randomSignature = ModuleUtils.generateRandomClassSignature(chosenOnt,1000);
//		System.out.println("Signature: " + randomSignature);
		System.out.println("Signaure Size: " + randomSignature);
		System.out.println("Ontology Size: " + chosenOnt.getLogicalAxiomCount());
		
		HashSet<OWLLogicalAxiom> module = null;
		try {
			System.out.println("Signature size " + ModuleUtils.getClassesInSet(alcAxioms).size());
			module = mod.extractModule(chosenOnt, ModuleUtils.getClassesInSet(alcAxioms));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (QBFSolverException e) {
			e.printStackTrace();
		}
		

		System.out.println("\nExtracted Module (" + module.size() + ") :");
		for(OWLLogicalAxiom ax: module){
			System.out.println(ax);
		}
		System.out.println("Size :" + module.size());
	}

}
