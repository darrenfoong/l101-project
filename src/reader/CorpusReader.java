package reader;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Iterator;

import utils.BiDistribution;
import utils.Distribution;
import utils.IntPair;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

import com.sun.org.apache.xalan.internal.utils.FeatureManager.Feature;

public abstract class CorpusReader {
	protected InstanceList featureInstances;
	protected InstanceList instances;
	
	protected HashMap<Integer, Distribution> xDistMap = new HashMap<Integer, Distribution>();
	protected HashMap<Integer, Distribution> cDistMap = new HashMap<Integer, Distribution>();
	protected HashMap<IntPair, BiDistribution> xcDistMap = new HashMap<IntPair, BiDistribution>();
	
	protected HashMap<Feature, Integer> features;

	public InstanceList getInstances() {
		return instances;
	}

	public abstract Pipe buildFeaturePipe(Alphabet dataAlphabet);

	public InstanceList readDirectory(File directory, Alphabet dataAlphabet) {
		return readDirectories(new File[] {directory}, dataAlphabet);
	}

	public InstanceList readDirectories(File[] directories, Alphabet dataAlphabet) {;
		FileIterator iter = new FileIterator(directories,
											new TxtFilter(),
											FileIterator.LAST_DIRECTORY);

		InstanceList tempInstances = new InstanceList(buildFeaturePipe(dataAlphabet));
		tempInstances.addThruPipe(iter);
		return tempInstances;
	}

	public void readFeatures() {
		featureInstances = readDirectories(null, null);

		Iterator<Instance> iter = featureInstances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector featureVector = (FeatureVector) instance.getData();

			cDistMap.get((Integer) instance.getName()).add((int) instance.getTarget());

			for ( int i = 0; i < featureVector.numLocations(); i++ ) {
				int featureID = featureVector.indexAtLocation(i);
				int featureValue = (int) featureVector.valueAtLocation(i);

				xDistMap.get(featureID).add(featureValue);
				xcDistMap.get(new IntPair(featureID, (int) instance.getName())).add(new IntPair(featureValue, (int) instance.getTarget()));
			}
		}
	}

	public void readInstances() {
		;
	}

	private class TxtFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return file.toString().endsWith(".txt");
		}
	}
}