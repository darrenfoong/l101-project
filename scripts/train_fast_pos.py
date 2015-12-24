#!/usr/bin/python

import sys
import nltk
from nltk.tag.perceptron import PerceptronTagger
from nltk.corpus import treebank

reload(sys)
sys.setdefaultencoding("utf8")

def retag(sentence):
  def retag_inner((w, pos)):
    new_pos = nltk.tag.mapping.map_tag("en-ptb", "universal", pos)
    if new_pos == "ADJ":
       new_pos = "a"
    elif new_pos == "VERB":
       new_pos = "v"
    else:
       new_pos = "n"
    return (w, new_pos)
  return map(retag_inner, sentence)

tagged_sents = treebank.tagged_sents()
tagged_sents = map(retag, list(tagged_sents))

tagger = PerceptronTagger(load=False)
PerceptronTagger().train(list(tagged_sents), save_loc="../data/fast_pos", nr_iter=10)
