#!/usr/bin/python

from email.parser import Parser
import os
import sys
import re
import cgi
import nltk
from nltk.stem import WordNetLemmatizer
from nltk.tag.perceptron import PerceptronTagger
import HTMLParser

reload(sys)
sys.setdefaultencoding("utf8")

# for speed
tagger = PerceptronTagger()

# read labels

labels_06 = {}
label_counter_06 = 0;
f_06 = open("../data/trec06p_org/full/index", "r")

for line in f_06:
  split_line = line.split(" ")
  label = split_line[0].lower()
  split_filename = split_line[1].split("/")
  # can assume that index file is really in sequence,
  # but this is safer
  num = int(split_filename[-2])*300 + int(split_filename[-1])
  labels_06[num] = label
  label_counter_06 += 1

f_06.close()
print "Read " + str(label_counter_06) + " labels for 2006; length of labels is " + str(len(labels_06)) + "."

labels_07 = {}
label_counter_07 = 0;
f_07 = open("../data/trec07p_org/full/index", "r")

for line in f_07:
  split_line = line.split(" ")
  label = split_line[0].lower()
  num = int(split_line[1].split(".")[-1])
  labels_07[num] = label
  label_counter_07 += 1

f_07.close()
print "Read " + str(label_counter_07) + " labels for 2007; length of labels is " + str(len(labels_07)) + "."

def remove_html(msg):
  style_re = re.compile(r'(<style>.*?</style>)',flags=re.DOTALL)
  msg = style_re.sub("", msg)
  tag_re = re.compile(r'(<!--.*?-->|<[^>]*>)')
  msg = tag_re.sub("", msg)
  return HTMLParser.HTMLParser().unescape(msg)

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

    # initially checked for "text/html"
    # but removed check because some e-mails are badly formatted
    # so just stripping everything of html
    return [remove_html(payload)]

def simplify_tag((w, pos)):
  if pos == "ADJ":
    return (w, "a")
  elif pos == "VERB":
    return (w, "v")
  else:
    return (w, "n")

def process(f, output):
  msg = Parser().parse(f)

  subject_line = msg.get("Subject", "").decode("utf8", "replace")

  contents = flatten(msg)
  new_lines = [subject_line+"\n"] + contents 

  # lemmatise

  full_message = "".join(new_lines)

  # nltk.pos_tag is very slow!
  full_message_tagged = nltk.tag._pos_tag(nltk.word_tokenize(full_message), "Universal", tagger)
  full_message_tagged = map(simplify_tag, full_message_tagged)
  full_message_lemm = map((lambda (w, pos): WordNetLemmatizer().lemmatize(w, pos)), full_message_tagged) 

  output.write(" ".join(full_message_lemm))

# 2007: starts from 1
for i in range(1,label_counter_07+1):
  print "Processing 2007 message #" + str(i)
  f = open("../data/trec07p_org/data/inmail." + str(i))
  output = open("../data/trec07p/test/" + labels_07[i] + "/" + str(i) + ".txt", "w") 

  process(f, output)
  output.close()
  f.close()

# 2006: starts from 0
for i in range(0, label_counter_06):
  print "Processing 2006 message #" + str(i+1)
  i_folder = i//300 # floor division
  i_file = i % 300
  f = open("../data/trec06p_org/data/" + str("{:03d}".format(i_folder)) + "/" + str("{:03d}".format(i_file)))
  output = open("../data/trec07p/train/" + labels_06[i] + "/" + str(i) + ".txt", "w") 

  process(f, output)
  output.close()
  f.close()
