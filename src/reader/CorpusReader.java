package reader;

import cc.mallet.types.InstanceList;

public abstract class CorpusReader {
	private InstanceList instances;

	public InstanceList getInstances() {
		return instances;
	}

	public abstract void read();
}