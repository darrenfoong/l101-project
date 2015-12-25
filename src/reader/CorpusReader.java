package reader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import utils.Distribution;
import utils.IntDoublePair;
import utils.IntStringPair;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public abstract class CorpusReader {
	protected final static short SPAM = 0;
	protected final static short HAM = 1;

	protected final static short SPAM0 = 0;
	protected final static short SPAM1 = 1;
	protected final static short HAM0 = 2;
	protected final static short HAM1 = 3;

	protected final int CUTOFF;

	protected InstanceList featureInstances;
	protected InstanceList dataInstances;

	protected Distribution cDist = null;
	protected Distribution[] xDists = null;
	protected Distribution[] xcDists = null;

	protected Alphabet newAlphabet;

	protected ArrayList<IntDoublePair> mutualInfo = new ArrayList<IntDoublePair>();

	protected int[] ns;

	protected CorpusReader(int cutoff, int[] ns) {
		CUTOFF = cutoff;
		if ( ns != null ) {
			this.ns = ns.clone();
		}
	}

	public InstanceList getFeatureInstances() {
		return featureInstances;
	}

	public InstanceList getDataInstances() {
		return dataInstances;
	}

	public abstract Pipe buildFeaturePipe(Alphabet dataAlphabet);

	public InstanceList readDirectory(File directory, Alphabet dataAlphabet) {
		return readDirectories(new File[] {directory}, dataAlphabet);
	}

	public InstanceList readDirectories(File[] directories, Alphabet dataAlphabet) {
		FileIterator iter = new FileIterator(directories,
											new TxtFilter(),
											FileIterator.LAST_DIRECTORY);

		InstanceList tempInstances = new InstanceList(buildFeaturePipe(dataAlphabet));
		tempInstances.addThruPipe(iter);
		return tempInstances;
	}

	public void readData(File[] directories) {
		System.out.println("Reading data from:");

		for ( File directory : directories ) {
			System.out.println("> " + directory.getPath());
		}

		dataInstances = readDirectories(directories, newAlphabet);

		System.out.println(" Number of data instances: " + dataInstances.size());

		System.out.println("Data read.");
	}

	public void readFeatures(File[] directories) {
		System.out.println("Reading features from:");

		for ( File directory : directories ) {
			System.out.println("> " + directory.getPath());
		}

		featureInstances = readDirectories(directories, null);

		int numFeatures = featureInstances.getDataAlphabet().size();

		System.out.println(" Number of feature instances: " + featureInstances.size());
		System.out.println(" Number of features: " + numFeatures);

		// printFeatures(featureInstances.getDataAlphabet());

		cDist = new Distribution(2);
		xDists = new Distribution[numFeatures];
		xcDists = new Distribution[numFeatures];

		for ( int i = 0; i < numFeatures; i++ ) {
			xDists[i] = new Distribution(2);
			xcDists[i] = new Distribution(4);
		}

		Iterator<Instance> iter = featureInstances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector featureVector = (FeatureVector) instance.getData();

			String label = instance.getTarget().toString();

			cDist.add(labelToShort(label));

			// boolean[] denseArray = new boolean[numFeatures];

			for ( int i = 0; i < featureVector.numLocations(); i++ ) {
				int featureID = featureVector.indexAtLocation(i);

				xDists[featureID].add(1);
				xcDists[featureID].add(labelValueToShort(instance.getTarget().toString(), 1));

				// denseArray[featureID] = true;
			}

			/*
			for ( int i = 0; i < numFeatures; i++ ) {
				int featureValue = denseArray[i] ? 1 : 0;
				xDists[i].add(featureValue);
				xcDists[i].add(labelValueToShort(label, featureValue));
			}
			*/
		}

		balanceDistributions();

		System.out.println("Features read. Distributions created.");
	}

	public void computeMutualInfo() {
		// conventions:
		// 0 log 0 = 0
		// 0 log (0/0) = 0

		System.out.println("Computing mutual information.");

		mutualInfo.clear();

		int numFeatures = featureInstances.getDataAlphabet().size();

		for ( int featureID = 0; featureID < numFeatures; featureID++ ) {
			double spam0 = 0;
			if ( xcDists[featureID].getProb(SPAM0) != 0 ) {
				spam0 = xcDists[featureID].getProb(SPAM0) *
						( xcDists[featureID].getLogProb(SPAM0) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(0)));
			}

			double spam1 = 0;
			if ( xcDists[featureID].getProb(SPAM1) != 0 ){
				spam1 = xcDists[featureID].getProb(SPAM1) *
						( xcDists[featureID].getLogProb(SPAM1) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(1)));
			}

			double ham0 = 0;
			if ( xcDists[featureID].getProb(HAM0) != 0 ) {
				ham0 = xcDists[featureID].getProb(HAM0) *
						( xcDists[featureID].getLogProb(HAM0) - (cDist.getLogProb(HAM) + xDists[featureID].getLogProb(0)));
			}

			double ham1 = 0;
			if ( xcDists[featureID].getProb(HAM1) != 0 ) {
				ham1 = xcDists[featureID].getProb(HAM1) *
						( xcDists[featureID].getLogProb(HAM1) - (cDist.getLogProb(SPAM) + xDists[featureID].getLogProb(1)));
			}

			double mi = spam0 + spam1 + ham0 + ham1;
			assert !Double.isNaN(mi);

			mutualInfo.add(new IntDoublePair(featureID, mi));
		}

		Collections.sort(mutualInfo, new Comparator<IntDoublePair>(){
			@Override
			public int compare(IntDoublePair p1, IntDoublePair p2){
				// sort in descending order
				return Double.compare(p2.getY(), p1.getY());
		}});

		System.out.println("Mutual information computed and sorted.");
	}

	public void pruneAlphabet() {
		System.out.println("Pruning alphabet.");

		Alphabet currentAlphabet = featureInstances.getAlphabet();
		System.out.println(" Current alphabet has " + currentAlphabet.size() + " features.");

		int oldIndices[] = new int[CUTOFF];
		ArrayList<String> newFeatures = new ArrayList<String>();

		for ( int i = 0; i < CUTOFF; i++ ) {
			oldIndices[i] = mutualInfo.get(i).getX();
			newFeatures.add(currentAlphabet.lookupObject(oldIndices[i]).toString());
			// System.out.println(" Mutual info rank " + i + ": " + oldIndices[i] + " " + mutualInfo.get(i).getY());
		}

		newAlphabet = new Alphabet(newFeatures.toArray());
		newAlphabet.stopGrowth();
		// this prevents changes to newAlphabet in subsequent calls to constructor for FeatureVector

		System.out.println(" New alphabet has " + newAlphabet.size() + " features; cutoff is " + CUTOFF + ".");
		System.out.println("Alphabet pruned.");
	}

	public FeatureVector changeAlphabet(FeatureVector featureVector) {
		ArrayList<Integer> featureIndices = new ArrayList<Integer>();
		ArrayList<Double> featureValues = new ArrayList<Double>();

		// System.out.println("Old feature vector (" + featureVector.numLocations() + "/" + featureVector.getAlphabet().size() + "):");
		// System.out.println(" " + featureVectorToPrettyString(featureVector));

		for ( int i = 0; i < featureVector.numLocations(); i++ ) {
			int featureValue = (int) featureVector.valueAtLocation(i);

			int oldIndex = (int) featureVector.indexAtLocation(i);
			// TODO: danger of error code -1
			String value = (String) featureVector.getAlphabet().lookupObject(oldIndex);
			int newIndex = newAlphabet.lookupIndex(value);

			if ( newIndex != -1 ) {
				featureIndices.add(newIndex);
				featureValues.add((double) featureValue);
			}
		}

		assert featureIndices.size() == featureValues.size();

		int[] featureIndicesPrim = new int[featureIndices.size()];
		double[] featureValuesPrim = new double[featureValues.size()];

		for ( int i = 0; i < featureIndices.size(); i++ ) {
			featureIndicesPrim[i] = featureIndices.get(i);
			featureValuesPrim[i] = featureValues.get(i);
		}

		FeatureVector newFeatureVector = new FeatureVector(newAlphabet, featureIndicesPrim, featureValuesPrim);

		// System.out.println("New feature vector (" + newFeatureVector.numLocations() + "/" + newFeatureVector.getAlphabet().size() + "):");
		// System.out.println(" " + featureVectorToPrettyString(newFeatureVector));

		return newFeatureVector;
	}

	public void changeAllAlphabets() {
		System.out.println("Changing all alphabets.");

		InstanceList newInstances = new InstanceList(newAlphabet, featureInstances.getTargetAlphabet());

		Iterator<Instance> iter = featureInstances.iterator();

		while ( iter.hasNext() ) {
			Instance instance = iter.next();
			FeatureVector newFeatureVector = changeAlphabet((FeatureVector) instance.getData());
			instance.unLock();
			instance.setData(newFeatureVector);
			instance.lock();
			newInstances.add(instance);
		}

		featureInstances = newInstances;

		System.out.println("All alphabets changed.");
	}

	public void balanceDistributions() {
		int numSpam = cDist.getCount(SPAM);
		int numHam = cDist.getCount(HAM);
		int numInstances = numSpam + numHam;

		System.out.println("numInstances: " + numInstances);
		System.out.println("featureInstances size: " + featureInstances.size());
		assert numInstances == featureInstances.size();

		int numFeatures = featureInstances.getDataAlphabet().size();

		for ( int i = 0; i < numFeatures; i++ ) {
			// xDist
			xDists[i].add(0, numInstances - xDists[i].getCount(1));
			// xcDist
			xcDists[i].add(SPAM0, numSpam - xcDists[i].getCount(SPAM1));
			xcDists[i].add(HAM0, numHam - xcDists[i].getCount(HAM1));
		}
	}

	private String featureVectorToPrettyString(FeatureVector featureVector) {
		ArrayList<IntStringPair> indexValues = new ArrayList<IntStringPair>();

		for ( int i = 0; i < featureVector.numLocations(); i++ ) {
			int index = (int) featureVector.indexAtLocation(i);
			String value = (String) featureVector.getAlphabet().lookupObject(index);
			indexValues.add(new IntStringPair(index, value));
		}

		Collections.sort(indexValues, new Comparator<IntStringPair>(){
			@Override
			public int compare(IntStringPair p1, IntStringPair p2){
				return p1.getY().compareTo(p2.getY());
		}});

		String output = "";

		for ( IntStringPair p : indexValues ) {
			output += p.getY() + "(" + p.getX() + ") ";
		}

		return output;
	}

	public void printFeatures(Alphabet alphabet) {
		PrintWriter output = null;

		try {
			output = new PrintWriter(new BufferedWriter(new FileWriter("data/features.out")));
			int numFeatures = alphabet.size();
			for ( int i = 0; i < numFeatures; i++ ) {
				String featureName = (String) alphabet.lookupObject(i);
				output.println(featureName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if ( output != null ) {
				output.close();
			}
		}
	}

	private short labelToShort(String label) {
		if ( label.equals("spam") ) {
			return SPAM;
		} else if ( label.equals("ham") ) {
			return HAM;
		} else {
			return -1;
		}
	}

	private short labelValueToShort(String label, int value) {
		if ( label.equals("spam") ) {
			if ( value ==  0 ) {
				return SPAM0;
			} else if ( value == 1 ) {
				return SPAM1;
			} else {
				return -1;
			}
		} else if ( label.equals("ham")) {
			if ( value ==  0 ) {
				return HAM0;
			} else if ( value == 1 ) {
				return HAM1;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	private static class TxtFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return file.toString().endsWith(".txt");
		}
	}
}