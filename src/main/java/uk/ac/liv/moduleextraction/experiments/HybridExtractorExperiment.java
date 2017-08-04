package uk.ac.liv.moduleextraction.experiments;

import com.google.common.base.Stopwatch;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.liv.moduleextraction.extractor.AMEX;
import uk.ac.liv.moduleextraction.extractor.STARAMEXHybridExtractor;
import uk.ac.liv.moduleextraction.extractor.STARMEXHybridExtractor;
import uk.ac.liv.moduleextraction.util.CPUTicker;
import uk.ac.liv.moduleextraction.util.ModuleUtils;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HybridExtractorExperiment implements Experiment {

	private SyntacticLocalityModuleExtractor starExtractor;
	private STARAMEXHybridExtractor hybridAmexExtractor;
	private STARMEXHybridExtractor hybridMexExtractor;
	private int starSize = 0;
	private int hybridAmexSize = 0;
	private int hybridMexSize  = 0;
	private Set<OWLLogicalAxiom> starModule;
	private Set<OWLLogicalAxiom> hybridAmexModule;
	private Set<OWLLogicalAxiom> hybridMexModule;
	private OWLOntology ontology;
	Stopwatch hybridAmexWatch;
	Stopwatch hybridMexWatch;
	Stopwatch starWatch;
	private File location;
	private File sigLocation;

	private Logger logger = LoggerFactory.getLogger(HybridExtractorExperiment.class);

	public HybridExtractorExperiment(OWLOntology ont, File originalLocation) {
		this.ontology = ont;
		this.location = originalLocation;
		this.starExtractor = new SyntacticLocalityModuleExtractor(ont.getOWLOntologyManager(), ont, ModuleType.STAR);
	}

	@Override
	public void performExperiment(Set<OWLEntity> signature) {
		//Compute the star module on it's own

		logger.trace("Hybrid experiments starts for signature {}", signature);

		logger.trace("... Extracting the STAR module");
		starWatch = Stopwatch.createStarted(new CPUTicker());
		Set<OWLAxiom> starAxioms = starExtractor.extract(signature);
		starWatch.stop();

		starModule = ModuleUtils.getLogicalAxioms(starAxioms);

		starSize = starModule.size();

		//Begin with the STAR module as it's the basis of the hybrid process anyway
		logger.trace("... Extracting the StarAmexHybrid module");

		hybridAmexExtractor = new STARAMEXHybridExtractor(starModule);
		hybridAmexWatch = Stopwatch.createStarted(new CPUTicker());
		//And then the iterated one 
		hybridAmexModule = hybridAmexExtractor.extractModule(signature);
		hybridAmexWatch.stop();

		hybridAmexSize = hybridAmexModule.size();

		logger.trace("... Extracting the StarMexHybrid module");
		hybridMexExtractor = new STARMEXHybridExtractor(starModule);
		hybridMexWatch = Stopwatch.createStarted(new CPUTicker());
		//And then the iterated one
		hybridMexModule = hybridMexExtractor.extractModule(signature);
		hybridMexWatch.stop();
		hybridMexSize = hybridMexModule.size();
	}

	public int getHybridAmexSize(){
		return hybridAmexSize;
	}

	public int getHybridMexSize(){
		return hybridMexSize;
	}

	public int getStarSize(){
		return starSize;
	}

	public Set<OWLLogicalAxiom> getHybridAmexModule(){
		return hybridAmexModule;
	}

	public Set<OWLLogicalAxiom> getHybridMexModule(){
		return hybridMexModule;
	}

	public Set<OWLLogicalAxiom> getStarModule(){
		return starModule;
	}

	public Stopwatch getHybridAmexWatch() {
		return hybridAmexWatch;
	}


	public Stopwatch getHybridMexWatch() {
		return hybridMexWatch;
	}


	public Stopwatch getStarWatch() {
		return starWatch;
	}

	public int getHybridAmexAMEXExtractions(){ return hybridAmexExtractor.getAmexExtractions(); }
	public int getHybridAmexSTARExtractions(){ return hybridAmexExtractor.getStarExtractions(); }

	public int getHybridMexMEXExtractions(){ return hybridMexExtractor.getMexExtractions(); }
	public int getHybridMexSTARExtractions(){ return hybridMexExtractor.getStarExtractions(); }


	public void performExperiment(Set<OWLEntity> signature, File signatureLocation){
		this.sigLocation = signatureLocation;
		performExperiment(signature);
	}


	@Override
	public void writeMetrics(File experimentLocation) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(experimentLocation.getAbsoluteFile() + "/" + "experiment-results.csv", false));

		writer.write(
			"StarSize, " +
				"HybridAmexSize, isDifference, " +
				"HybridMexSize, isDifference, " +
				"AmexStarExtractions, AmexAmexExtractions, " +
				"MexStarExtractions, MexMexExtractions, " +
				"StarTime, " +
				"HybridAmexTime, " +
				"HybridMexTime,  " +
				"OntLocation, SigLocation" + "\n");
		writer.write(
			starSize + "," +
				hybridAmexSize + "," + ((starSize == hybridAmexSize) ? "0" : "1") + ", " +
				hybridMexSize  + "," + ((hybridAmexSize == hybridMexSize) ? "0" : "1") + ", " +
				hybridAmexExtractor.getStarExtractions() + "," + hybridAmexExtractor.getAmexExtractions() + ", " +
				hybridMexExtractor.getStarExtractions() + "," + hybridMexExtractor.getMexExtractions() + ", " +
				starWatch.elapsed(TimeUnit.MILLISECONDS) + ", " +
				hybridAmexWatch.elapsed(TimeUnit.MILLISECONDS) + ", " +
				hybridMexWatch.elapsed(TimeUnit.MILLISECONDS) + ", " +
				location.getAbsolutePath() + "," + sigLocation.getAbsolutePath() + "\n");
		writer.flush();
		writer.close();

	}
	
	public void printMetrics(){
		System.out.print("StarSize, IteratedSize, Difference, StarExtractions, AmexExtractions, StarTime, HybridTime" + "\n");
		System.out.print(starSize + "," + hybridAmexSize + "," + ((starSize == hybridAmexSize) ? "0" : "1") + "," +
				hybridAmexExtractor.getStarExtractions() + "," + hybridAmexExtractor.getAmexExtractions()
				+ "," + 	starWatch.toString() + "," + hybridAmexWatch.toString() + "\n");

	}



}
