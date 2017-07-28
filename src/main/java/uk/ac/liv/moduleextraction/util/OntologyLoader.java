package uk.ac.liv.moduleextraction.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OntologyLoader {

	public static OWLOntology loadOntologyAllAxioms(String pathName){

		ToStringRenderer stringRenderer= new ToStringRenderer();
		stringRenderer.setRenderer(() -> new DLSyntaxObjectRenderer());


		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
		// OWLOntologyLoaderConfiguration is immutable. Must use the returned value
		configuration = configuration.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

		OWLOntology ontology = null;
		
		//FileDocumentSource source = new FileDocumentSource(new File(pathName));
		try {
            // InputStream source = new FileInputStream(pathName);
		    InputStream source = null;
		    String extension = FilenameUtils.getExtension(pathName);
		    switch (extension) {
                case "bz2":
                    source = new BZip2CompressorInputStream(new FileInputStream(pathName));
                    break;
                case "zip":
                    source = new ZipArchiveInputStream(new FileInputStream(pathName));
                    break;
                default:
                    // assume plain text
                    source = new FileInputStream(pathName);
            }
            MissingImportListener listener = importer -> System.out.println("Missing import");
			manager.addMissingImportListener(listener);

			try {
				manager.setOntologyLoaderConfiguration(configuration);
				ontology =
						manager.loadOntologyFromOntologyDocument(source); //, configuration

			} catch (UnparsableOntologyException e) {
				System.out.println("Unparsable ontology " + pathName);
				e.printStackTrace();
			} catch (UnloadableImportException importe) {
				System.out.println("Unloadable Import in " + pathName);
				importe.printStackTrace();
			} catch (OWLOntologyCreationException e) {
				System.out.println("Creation failed: " + e.getCause());
				e.printStackTrace();
			}
		}
		catch (IOException e) {
			System.out.print("File " + pathName + " does not exist");
			e.printStackTrace();
		}
		
		

		return ontology;
	}
	

	public static void main(String[] args) {
		OWLOntology ont = OntologyLoader.loadOntologyAllAxioms(ModulePaths.getOntologyLocation() + "/OWL-Corpus-All/qbf-only/3");
		System.out.println(ont.getLogicalAxiomCount());
			
		
	}


}
