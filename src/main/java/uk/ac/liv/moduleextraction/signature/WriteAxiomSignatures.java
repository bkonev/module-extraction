package uk.ac.liv.moduleextraction.signature;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import uk.ac.liv.moduleextraction.util.ModulePaths;
import uk.ac.liv.moduleextraction.util.OntologyLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WriteAxiomSignatures {

	File saveLocation;
	Set<OWLLogicalAxiom> axioms;
	public WriteAxiomSignatures(OWLOntology ontology, File saveLocation) {
		this.axioms = OWLAPIStreamUtils.asSet(ontology.logicalAxioms());
		this.saveLocation = saveLocation;
	}
	
	public WriteAxiomSignatures(Set<OWLLogicalAxiom> axioms, File saveLocation){
		this.axioms = axioms;
		this.saveLocation = saveLocation;
	}

	public Set<Set<OWLEntity>> getAxiomSignatures() {
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		Set<Set<OWLEntity>> result = new HashSet<>();
		for(OWLLogicalAxiom axiom : axioms){
			Set<OWLEntity> signature = OWLAPIStreamUtils.asSet(axiom.signature());
			signature.remove(factory.getOWLThing());
			signature.remove(factory.getOWLNothing());
			result.add(signature);
		}
		return result;
	}
	
	public void writeAxiomSignatures(){
		OWLDataFactory factory = OWLManager.getOWLDataFactory();
		SigManager sigmanager = new SigManager(saveLocation);
		int i = 0;
		for(Set<OWLEntity> sig : getAxiomSignatures()) {
			i++;
			try {
				//Give each axiom a unique file name
				sigmanager.writeFile(sig, "axiom" + sig.toString().hashCode());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		System.out.println("Written " + i + " signatures");
	}
	
	
	public static void main(String[] args) throws OWLOntologyCreationException {



		File[] files = new File(ModulePaths.getOntologyLocation()).listFiles();
		int i = 1;
		for(File f : files){
			System.out.println("Expr: " + i++);
			if(f.exists()){
				System.out.print(f.getName() + ": ");
				OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(f.getAbsolutePath());
				 WriteAxiomSignatures writer = new WriteAxiomSignatures(ont,
						 new File(ModulePaths.getSignatureLocation() + f.getName()));
				   writer.writeAxiomSignatures();
				   ont.getOWLOntologyManager().removeOntology(ont);
			}
		}

	}

}
