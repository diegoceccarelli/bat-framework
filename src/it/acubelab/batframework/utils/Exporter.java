/**
 * (C) Copyright 2012-2013 A-cube lab - Università di Pisa - Dipartimento di Informatica. 
 * BAT-Framework is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * BAT-Framework is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with BAT-Framework.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.acubelab.batframework.utils;

import it.acubelab.batframework.data.Annotation;
import it.acubelab.batframework.data.Tag;
import it.acubelab.batframework.problems.A2WDataset;
import it.acubelab.batframework.problems.C2WDataset;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

public class Exporter {

	/**
	 * Export an A2W dataset in readable XML format.
	 * @param ds the dataset to export.
	 * @param outputStream the stream to write the XML to.
	 * @param api an API to Wikipedia (needed for retrieving information about the annotations).
	 * @throws DOMException if something went wrong with the XML.
	 * @throws IOException if something went wrong with the output.
	 */
	public static void exportA2WDataset(A2WDataset ds, OutputStream outputStream, WikipediaApiInterface api) throws DOMException, IOException{
		exportDataset(ds.getName(), ds.getA2WGoldStandardList(), ds.getTextInstanceList(), outputStream, api, new A2WPopulator());
	}

	/**
	 * Export a C2W dataset in readable XML format.
	 * @param ds the dataset to export.
	 * @param outputStream the stream to write the XML to.
	 * @param api an API to Wikipedia (needed for retrieving information about the tags).
	 * @throws DOMException if something went wrong with the XML.
	 * @throws IOException if something went wrong with the output.
	 */
	public static void exportC2WDataset(C2WDataset ds, OutputStream outputStream, WikipediaApiInterface api) throws DOMException, IOException{
		exportDataset(ds.getName(), ds.getC2WGoldStandardList(), ds.getTextInstanceList(), outputStream, api, new C2WPopulator());
	}

	private static <A extends Tag> void exportDataset(String datasetName, List<Set<A>> list, List<String> texts, OutputStream outputStream, WikipediaApiInterface api, Populator<A> p) throws DOMException, IOException{

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("dataset");
		rootElement.setAttribute("name", datasetName);
		doc.appendChild(rootElement);

		p.populateDoc(texts, doc, rootElement, list, api);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			//			StreamResult result = new StreamResult(new File("C:\\file.xml"));

			StreamResult result = new StreamResult(outputStream);

			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}


	private static interface Populator <A extends Tag> {
		public void populateDoc(List<String> texts, Document doc, Element rootElement, List<Set<A>> list, WikipediaApiInterface api) throws DOMException, IOException;			
	}

	private static class A2WPopulator implements Populator<Annotation> {
		public void populateDoc(List<String> texts, Document doc, Element rootElement, List<Set<Annotation>> list, WikipediaApiInterface api) throws DOMException, IOException{
			for(int i=0 ; i<texts.size(); i++){
				Element instance = doc.createElement("instance");
				rootElement.appendChild(instance);
				String body = texts.get(i);
				List<Annotation> annsList = new Vector<Annotation>(Annotation.deleteOverlappingAnnotations(list.get(i)));
				Collections.sort(annsList);
				int prevPos = 0;
				for (Annotation a : annsList){
					instance.appendChild(doc.createTextNode(body.substring(prevPos, a.getPosition())));
					Element ann = doc.createElement("tag");
					instance.appendChild(ann);
					ann.setAttribute("wid", Integer.toString(a.getConcept()));
					ann.setAttribute("title", api.getTitlebyId(a.getConcept()));
					ann.appendChild(doc.createTextNode(body.substring(a.getPosition(), a.getLength()+a.getPosition())));

					prevPos = a.getPosition() + a.getLength();
				}
				instance.appendChild(doc.createTextNode(body.substring(prevPos)));
			}
		}
	}

	private static class C2WPopulator implements Populator<Tag>{
		public void populateDoc(List<String> texts, Document doc, Element rootElement, List<Set<Tag>> list, WikipediaApiInterface api) throws DOMException, IOException{
			for(int i=0 ; i<texts.size(); i++){
				Element instance = doc.createElement("instance");
				rootElement.appendChild(instance);
				
				Element text = doc.createElement("text");
				text.setTextContent(texts.get(i));
				instance.appendChild(text);
				
				for (Tag a : list.get(i)){
					Element tag = doc.createElement("tag");
					instance.appendChild(tag);
					tag.setAttribute("wid", Integer.toString(a.getConcept()));
					tag.setAttribute("title", api.getTitlebyId(a.getConcept()));
				}
			}
		}
	}
}