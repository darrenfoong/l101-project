# L101 Project

## Requirements

- Java
- Python
- NLTK
- MALLET

## MALLET

http://mallet.cs.umass.edu/download.php

Place `mallet.jar` and `mallet-deps.jar` in a new top-level `lib` directory.

## Pre-processing

It is recommended to create a top-level `data` directory with the following structure

```
data
|-bnc
|-genspam
 |-...
|-genspam_org // extract GenSpam.tar.gz here
|-pu1         // extract pu1_encoded.tar.tar here
|-trec07p
 |-...
|-trec06p_org // extract trec06p.tgz here
|-trec07p_org // extract trec07p.tgz here
```

### PU1 corpus:

http://www.csmining.org/index.php/pu1-and-pu123a-datasets.html

The PU1 corpus requires no pre-processing.

### GenSpam corpus

http://www.benmedlock.co.uk/genspam.html

Create the following directory structure under `data/genspam/`

```
|-genspam
 |-bare
  |-test
   |-ham
   |-spam
  |-train
   |-ham
   |-spam
 |-lemm
  |-test
   |-ham
   |-spam
  |-train
   |-ham
   |-spam
```

Run `scripts/genspam_convert.py`, which will populate the folders above.

### TREC 2006/2007 public corpora

http://plg.uwaterloo.ca/~gvcormac/treccorpus06/, http://plg.uwaterloo.ca/~gvcormac/treccorpus07/

Create the following directory structure under `data/trec07p/`

```
|-trec07p
 |-bare
  |-test
   |-ham
   |-spam
  |-train
   |-ham
   |-spam
 |-lemm
  |-test
   |-ham
   |-spam
  |-train
   |-ham
   |-spam
```

Run `scripts/trec07p_convert.py`, which will populate the folders above.

## Stoplist

The `scripts/get_top.py` script extracts the top N most frequent distinct words from `data/bnc/all.num`, which can be obtained [here](http://www.kilgarriff.co.uk/bnc-readme.html). The script produces the stoplist `data/bnc/topN`, which can be used by the spam filter.

## Running

```
java -classpath bin:lib/mallet.jar:lib/mallet-deps.jar NaiveBayesClassifier $CORPUS $VERSION $CUTOFF $LAMBDA > output/run.log
```

where `$CORPUS` ranges over `pu1`, `genspam`, and `trec07p`; `$VERSION` ranges over `bare`, `stop`, `lemm`, `lemm_stop`; `$CUTOFF` is an integer and `$LAMBDA` is a double.
