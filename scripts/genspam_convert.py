#!/usr/bin/python

from xml.dom import minidom
import os
import codecs
import sys

reload(sys)
sys.setdefaultencoding("utf8")

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

toRemove1 = ["&NAME", "&NUM", "&WEBSITE", "&CHAR", "&EMAIL", "&ORG", "&SMILEY"]
toRemove2 = ["[[:cntrl:]]","<a href=\">"]

patterns = []

def genRemovePattern(removePatterns):
  pattern = "s/";

  for e in removePatterns[:-1]:
    pattern += "\(" + e + "\)\|"

  pattern += "\(" + removePatterns[-1] + "\)//g"

  return pattern

patterns.append(genRemovePattern(toRemove1))
patterns.append(genRemovePattern(toRemove2))
patterns.append("s/&/&amp;/g")
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

for fkey in data.keys():
  fname = data[fkey] + TMP_EXT
  f = codecs.open(fname, 'r', 'utf8', 'replace')
  print "Parsing " + fname

  doc = minidom.parse(f)
  messageList = doc.getElementsByTagName("MESSAGE")

  numMessages = len(messageList)
  print fkey + ": " + str(numMessages) + " messages;",
  if numMessages == counts[fkey]:
    print "correct."
  else:
    print "wrong."

  for i, message in enumerate(messageList, 1):
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

    messageBody = message.getElementsByTagName("MESSAGE_BODY")
    if messageBody:
      messageBody_inner = messageBody[0].getElementsByTagName("TEXT_NORMAL")
      if messageBody_inner:
        messageBody_text = messageBody_inner[0].firstChild.nodeValue
      else:
        messageBody_text = ""
    else:
      messageBody_text = ""

    output.write(subject_text)
    output.write("\n\n")
    output.write(messageBody_text)
    output.close()

  print "Generated individual files for " + fkey + " in " + output_folders[fkey] + "."

# cleaning up

for fname in data.values():
  fname += TMP_EXT
  print "Deleting " + fname
  os.system("rm " + fname)
