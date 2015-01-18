package uk.ac.liv.moduleextraction.extractor;

import com.google.common.base.Stopwatch;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.liv.moduleextraction.metrics.ExtractionMetric;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class EquivalentToTerminologyExtractor implements Extractor {

	private EquivalentToTerminologyProcessor processor;
	private Set<OWLLogicalAxiom> module;
	private AMEX extractor;
	
	private long timeTaken;

	public EquivalentToTerminologyExtractor(OWLOntology equivalentToTerm) {
		try {
			processor = new EquivalentToTerminologyProcessor(equivalentToTerm);
			OWLOntology newOnt = processor.getConvertedOntology();
			extractor = new AMEX(newOnt);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (NotEquivalentToTerminologyException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Set<OWLLogicalAxiom> extractModule(Set<OWLLogicalAxiom> existingModule, Set<OWLEntity> signature) {
		Stopwatch stopwatch = new Stopwatch().start();
		module =  extractor.extractModule(existingModule, signature);
		module = processor.postProcessModule(module);
		stopwatch.stop();
		timeTaken = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		return module;
	}

	public Set<OWLLogicalAxiom> extractModule(Set<OWLEntity> signature) {
		return extractModule(new HashSet<OWLLogicalAxiom>(), signature);
	}

	public ExtractionMetric getMetrics(){
		ExtractionMetric amexMetric = extractor.getMetrics();
		ExtractionMetric.MetricBuilder builder =
				new ExtractionMetric.MetricBuilder(ExtractionMetric.ExtractionType.AMEX);
		builder.timeTaken(timeTaken);
		builder.moduleSize(module.size());
		builder.syntacticChecks(amexMetric.getSyntacticChecks());
		builder.qbfChecks(amexMetric.getQbfChecks());
		builder.separabilityCausingAxioms(amexMetric.getSeparabilityAxiomCount());
		return builder.createMetric();

	}


	public LinkedHashMap<String, Long> getQBFMetrics() {
		return extractor.getQBFMetrics();
	}
	




}
