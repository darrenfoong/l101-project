#!/usr/bin/python

from xml.dom import minidom
import os
import codecs
import sys
import nltk
from nltk.stem import WordNetLemmatizer
from nltk.tag.perceptron import PerceptronTagger
import HTMLParser

reload(sys)
sys.setdefaultencoding("utf8")

# for speed
tagger = PerceptronTagger()

# constants

TMP_EXT = ".tmp"

# input data

data = {
  "train_spam": "../data/genspam_org/train_SPAM.ems",
  "train_ham": "../data/genspam_org/train_GEN.ems",
  "test_spam": "../data/genspam_org/test_SPAM.ems",
  "test_ham": "../data/genspam_org/test_GEN.ems" }

counts = {
  "train_spam": 30099,
  "train_ham": 8158,
  "test_spam": 797,
  "test_ham": 754 }

output_folders = {
  "train_spam": "../data/genspam/train/spam/",
  "train_ham": "../data/genspam/train/ham/",
  "test_spam": "../data/genspam/test/spam/",
  "test_ham": "../data/genspam/test/ham/" }

# substitutions

to_remove1 = ["&NAME", "&NUM", "&WEBSITE", "&CHAR", "&EMAIL", "&ORG", "&SMILEY"]
to_remove2 = ["[[:cntrl:]]","<a href=\">"]

patterns = []

def gen_remove_pattern(remove_patterns):
  pattern = "s/";

  for e in remove_patterns[:-1]:
    pattern += "\(" + e + "\)\|"

  pattern += "\(" + remove_patterns[-1] + "\)//g"

  return pattern

patterns.append(gen_remove_pattern(to_remove1))
patterns.append(gen_remove_pattern(to_remove2))
patterns.append("s/&/GENSPAM_AMPERSAND/g")
patterns.append("s/Message-ID: </Message-ID: /g")

for fname in data.values():
  command = "sed -e '"

  for pattern in patterns[:-1]:
    command += pattern + "' -e '"

  command += patterns[-1] + "' " + fname + " > " + fname + TMP_EXT

  # for above patterns
  os.system(command)
  # for adding top-level element; minidom XML issue
  os.system("sed -i '1i <TOPLEVEL>' " + fname + TMP_EXT)
  os.system("sed -i '$a </TOPLEVEL>' " + fname + TMP_EXT)

# reading

def simplify_tag((w, pos)):
  if pos == "ADJ":
    return (w, "a")
  elif pos == "VERB":
    return (w, "v")
  else:
    return (w, "n")

for fkey in data.keys():
  fname = data[fkey] + TMP_EXT
  f = codecs.open(fname, 'r', 'utf8', 'replace')
  print "Parsing " + fname

  doc = minidom.parse(f)
  message_list = doc.getElementsByTagName("MESSAGE")

  num_messages = len(message_list)
  print fkey + ": " + str(num_messages) + " messages;",
  if num_messages == counts[fkey]:
    print "correct."
  else:
    print "wrong."

  for i, message in enumerate(message_list, 1):
    output = open(output_folders[fkey] + str(i) + ".txt", "w")

    subject = message.getElementsByTagName("SUBJECT")
    if subject:
      subject_inner = subject[0].getElementsByTagName("TEXT_NORMAL")
      if subject_inner:
        subject_text = subject_inner[0].firstChild.nodeValue
      else:
        subject_text = ""
    else:
      subject_text = ""

    message_body = message.getElementsByTagName("MESSAGE_BODY")
    if message_body:
      message_body_inner = message_body[0].getElementsByTagName("TEXT_NORMAL")
      if message_body_inner:
        message_body_text = message_body_inner[0].firstChild.nodeValue
      else:
        message_body_text = ""
    else:
      message_body_text = ""

    # lemmatise

    full_message = subject_text + " " + message_body_text 
    full_message = full_message.replace("GENSPAM_AMPERSAND", "&")
    full_message = HTMLParser.HTMLParser().unescape(full_message)

    # nltk.pos_tag is very slow!
    full_message_tagged = nltk.tag._pos_tag(nltk.word_tokenize(full_message), "Universal", tagger)
    full_message_tagged = map(simplify_tag, full_message_tagged)
    full_message_lemm = map((lambda (w, pos): WordNetLemmatizer().lemmatize(w, pos)), full_message_tagged)

    output.write(" ".join(full_message_lemm))
    output.close()

  print "Generated individual files for " + fkey + " in " + output_folders[fkey] + "."

# cleaning up

for fname in data.values():
  fname += TMP_EXT
  print "Deleting " + fname
  os.system("rm " + fname)
