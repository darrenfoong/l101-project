#!/usr/bin/python

from email.parser import Parser
import os
import sys
import re
import cgi
import nltk
from nltk.stem import WordNetLemmatizer
from nltk.tag.perceptron import PerceptronTagger

# for speed
tagger = PerceptronTagger()

reload(sys)
sys.setdefaultencoding("utf8")

# input data

output_folders = {
  "train_spam": "../data/trec07p/train/spam/",
  "train_ham": "../data/trec07p/train/ham/",
  "test_spam": "../data/trec07p/test/spam/",
  "test_ham": "../data/trec07p/test/ham/" }

# read labels

labels = {}
label_counter = 0;
f = open("../data/trec07p_org/full/index", "r")

for line in f:
  split_line = line.split(" ")
  label = split_line[0].lower()
  num = int(split_line[1].split(".")[-1])
  labels[num] = label
  label_counter += 1

f.close()
print "Read " + str(label_counter) + " labels; length of labels is " + str(len(labels)) + "."

def remove_html(msg):
  tag_re = re.compile(r'(<!--.*?-->|<[^>]*>)')
  no_tags = tag_re.sub('', msg)
  return cgi.escape(no_tags)

def flatten(msg):
  if msg.is_multipart():
    mlist =  map((lambda m: flatten(m)), msg.get_payload())
    return [item for sublist in mlist for item in sublist]
  else:
    charset = msg.get_param("charset")
    # print "Charset: " + str(charset)
    payload = msg.get_payload()
    if charset != None:
      try:
        payload = payload.decode(charset, "replace")
      except LookupError:
        payload = msg.get_payload().decode("utf8", "replace")
    else:
      payload = payload.decode("utf8", "replace") 

    if msg.get_content_type() == "text/html":
      return [remove_html(payload)]
    else:
      return [payload]

def simplify_tag(w, pos):
  if pos == "ADJ":
    return (w, "a")
  elif pos == "VERB":
    return (w, "v")
  else:
    return (w, "n")

for i in range(1,label_counter+1):
  print "Processing message #" + str(i)
  f = open("../data/trec07p_org/data/inmail." + str(i))
  output = open("../data/trec07p/test/" + labels[i] + "/" + str(i) + ".txt", "w") 
  msg = Parser().parse(f)

  subject_line = msg.get("Subject", "").decode("utf8", "replace")

  contents = flatten(msg)
  new_lines = [subject_line+"\n"] + contents 
  # new_lines = map((lambda s: s.split("\n")), new_lines)
  # new_lines = [item for sublist in new_lines for item in sublist]
  # new_lines = map((lambda s: s + "\n"), new_lines)

  # lemmatise

  full_message = "".join(new_lines)

  def g((w, pos)):
    # print w
    return WordNetLemmatizer().lemmatize(w, pos)

  # nltk.pos_tag is very slow!
  full_message_tagged = nltk.tag._pos_tag(nltk.word_tokenize(full_message), "Universal", tagger)
  full_message_tagged = map((lambda (w, pos): simplify_tag(w, pos)), full_message_tagged)
  full_message_lemm = map(g, full_message_tagged) 

  output.write(" ".join(full_message_lemm))
  output.close()
  f.close()
