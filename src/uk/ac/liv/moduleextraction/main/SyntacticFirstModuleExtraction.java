package uk.ac.liv.moduleextraction.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.liv.moduleextraction.chaindependencies.ChainDependencies;
import uk.ac.liv.moduleextraction.checkers.InseperableChecker;
import uk.ac.liv.moduleextraction.checkers.LHSSigExtractor;
import uk.ac.liv.moduleextraction.checkers.SyntacticDependencyChecker;
import uk.ac.liv.moduleextraction.datastructures.LinkedHashList;
import uk.ac.liv.moduleextraction.qbf.QBFSolverException;
import uk.ac.liv.moduleextraction.signature.SigManager;
import uk.ac.liv.moduleextraction.signature.SignatureGenerator;
import uk.ac.liv.moduleextraction.testing.AlternativeApproach;
import uk.ac.liv.moduleextraction.util.AxiomComparator;
import uk.ac.liv.moduleextraction.util.DefinitorialDepth;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.liv.ontologyutils.loader.OntologyLoader;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

public class SyntacticFirstModuleExtraction {

	/* Syntactic Checking */
	private SyntacticDependencyChecker syntaxDepChecker = new SyntacticDependencyChecker();

	/* Semantic Checking */
	private LHSSigExtractor lhsExtractor = new LHSSigExtractor();
	private InseperableChecker insepChecker = new InseperableChecker();

	/* Data Structures */
	private LinkedHashList<OWLLogicalAxiom> terminology;
	private Set<OWLLogicalAxiom> module;
	private Set<OWLEntity> signature;
	private HashSet<OWLEntity> sigUnionSigM;

	/* For writing sigs that cause inseperability */
	SigManager sigManager = new SigManager(new File(ModulePaths.getSignatureLocation() + "/insepSigs"));

	public SyntacticFirstModuleExtraction(Set<OWLLogicalAxiom> terminology, Set<OWLEntity> signature) {
		this(terminology, null, signature);
	}


	public SyntacticFirstModuleExtraction(Set<OWLLogicalAxiom> term, Set<OWLLogicalAxiom> existingModule, Set<OWLEntity> sig) {
		ArrayList<OWLLogicalAxiom> listOfAxioms = new ArrayList<OWLLogicalAxiom>(term);
		HashMap<OWLClass, Integer> definitorialMap = new DefinitorialDepth(term).getDefinitorialMap();
		Collections.sort(listOfAxioms, new AxiomComparator(definitorialMap));


		this.terminology = new LinkedHashList<OWLLogicalAxiom>(listOfAxioms);
		this.signature = sig;
		this.module = (existingModule == null) ? new HashSet<OWLLogicalAxiom>() : existingModule;
	
		
		populateSignature();
	}


	public LinkedHashList<OWLLogicalAxiom> getTerminology() {
		return terminology;
	}

	public Set<OWLLogicalAxiom> getModule() {
		return module;
	}



	private void populateSignature() {
		sigUnionSigM = new HashSet<OWLEntity>();
		sigUnionSigM.addAll(signature);
	}


	public Set<OWLLogicalAxiom> extractModule() throws IOException, QBFSolverException{
		collectSyntacticDependentAxioms();

		ChainDependencies tminusMDependencies = new ChainDependencies();
		tminusMDependencies.updateDependenciesWith(terminology);
		HashSet<OWLLogicalAxiom> lhsSigT = lhsExtractor.getLHSSigAxioms(terminology,sigUnionSigM,tminusMDependencies);

		if(insepChecker.isSeperableFromEmptySet(lhsSigT, sigUnionSigM)){
//			sigManager.writeFile(signature, "insep" + Math.abs(signature.hashCode()));
//			collectSemanticDependentAxioms();
			AlternativeApproach search = new AlternativeApproach(terminology, module, signature);
			OWLLogicalAxiom insepAxiom = search.getInseperableAxiom();
			//System.out.println("Adding: " + insepAxiom);
			module.add(insepAxiom);
			sigUnionSigM.addAll(insepAxiom.getSignature());
			terminology.remove(insepAxiom);
			extractModule();
		}

		return module;
	}

	private void collectSyntacticDependentAxioms() {
		System.out.println("Collecting Syntactic dependent axioms");
		LinkedHashList<OWLLogicalAxiom> W  = new LinkedHashList<OWLLogicalAxiom>();
		Iterator<OWLLogicalAxiom> axiomIterator = terminology.iterator();
		ChainDependencies syntaticDependencies = new ChainDependencies();

		int addedCount = 0;
		/* Terminology is the value of T\M as we remove items and add them to the module */
		while(!(terminology.size() == W.size())){
			OWLLogicalAxiom chosenAxiom = axiomIterator.next();

			W.add(chosenAxiom);
			

			syntaticDependencies.updateDependenciesWith(chosenAxiom);
			

			if(syntaxDepChecker.hasSyntacticSigDependency(W, syntaticDependencies, sigUnionSigM)){
		
				Set<OWLLogicalAxiom> axiomsWithDeps = syntaxDepChecker.getAxiomsWithDependencies();
				module.addAll(axiomsWithDeps);
				addedCount += axiomsWithDeps.size();
				terminology.removeAll(axiomsWithDeps);
				sigUnionSigM.addAll(ModuleUtils.getClassAndRoleNamesInSet(axiomsWithDeps));

				W.clear();
				/* reset the iterator */
				axiomIterator = terminology.iterator();
			}
		}
		if(addedCount > 0)
			System.out.println("Adding " + addedCount + " axiom(s) to module");
	}


	public static void main(String[] args) {

		OWLOntology ont = OntologyLoader.loadOntology(ModulePaths.getOntologyLocation() + "interp/semanticdep.krss");
		System.out.println("Loaded Ontology");

		System.out.println(ont);
		
		SignatureGenerator gen = new SignatureGenerator(ont.getLogicalAxioms());
		SigManager sigManager = new SigManager(new File(ModulePaths.getSignatureLocation() + "/insepSigs"));
		
		OWLDataFactory f = OWLManager.getOWLDataFactory();
		
		


		OWLClass lion = f.getOWLClass(IRI.create(ont.getOntologyID() + "#Lion"));
		OWLClass dog = f.getOWLClass(IRI.create(ont.getOntologyID() + "#Dog"));
		OWLClass fox = f.getOWLClass(IRI.create(ont.getOntologyID() + "#Fox"));
		
		Set<OWLEntity> signature = new HashSet<OWLEntity>();
		signature.add(fox);
		signature.add(lion);
		signature.add(dog);
	
		
			Set<OWLEntity> sig = signature;

			SyntacticLocalityModuleExtractor syntaxModExtractor = 
					new SyntacticLocalityModuleExtractor(OWLManager.createOWLOntologyManager(), ont, ModuleType.STAR);
			Set<OWLLogicalAxiom> starModule = ModuleUtils.getLogicalAxioms(syntaxModExtractor.extract(sig));
		
			int starSize = starModule.size();


			Set<OWLLogicalAxiom> syntfirstExtracted = null;
			System.out.println("|Signature|: " + sig);

			try {
				long startTime = System.currentTimeMillis();
				SyntacticFirstModuleExtraction syntmod = new SyntacticFirstModuleExtraction(ont.getLogicalAxioms(), sig);
				syntfirstExtracted = syntmod.extractModule();
				System.out.println("Time taken: " + ModuleUtils.getTimeAsHMS(System.currentTimeMillis() - startTime));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (QBFSolverException e) {
				e.printStackTrace();
			}

			System.out.println("Star module size: " + starSize);
			System.out.println("Synsize: " + syntfirstExtracted.size());
			//System.out.println("QBF Checks " + InseperableChecker.getTestCount());
			System.out.println();
		}
	
	


	//	}
}
