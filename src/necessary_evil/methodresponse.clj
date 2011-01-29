(ns necessary-evil.methodresponse
  "This module implementents the methodResponse wire format portion of the
   XML-RPC spec. You can find the official spec at
   http://www.xml-rpc.com/spec.

   A methodResponse may contain either a value or a fault. Unless you are
   extending this you should only need the emit-method-respons and fault
   function.

   Use clojure.xml/emit to turn the xml structure returned into text. As emit
   prints to *out* you may need to use with-out-str to capture the result.
   "
  (:use [clojure.contrib.zip-filter.xml :only [xml-> xml1->]]
        [necessary-evil.value :only [parse-value value-elem]]
        [necessary-evil.fault :only [fault]]
        [necessary-evil.xml-utils])
  (:require [clojure.zip :as zip])
  (:import necessary-evil.fault.Fault))

;; deserialising methodResponse XML to clojure structures

(defn method-response?
  "a predicate to ensure that this xml structure is a <methodResponse>."
  [x] (-> x zip/node :tag (= :methodResponse)))

(defn parse-fault
  [x] (let [f (parse-value x)
            fault-code (or (:faultCode f) -1)
            fault-message (or (:faultString f) -1)]
        (fault fault-code (.trim fault-message))))

(defn parse-response-content
  "Returns a clojure datastructure there is <params>,
   a Fault record if there is a <fault> or nil otherwise."
  [x] (condp = (:tag (zip/node x))
          :params (parse-value (xml1-> x :params :param :value first-child))
          :fault (parse-fault (xml1-> x :fault :value first-child))))

(defn parse
  "Converts an XML structure representing an RPC result into either a data
   structure or a Fault record."
  [x] (when (method-response? x)
        (parse-response-content (xml1-> x first-child))))

;; serialising methodResponses to XML

(defprotocol ResponseElements
  "This protocol is used to generate the correct nodes for a value or a
   fault."
  (response-elem [v]))

(extend-protocol ResponseElements
  Fault
  (response-elem
   [fault] (let [fault-struct {:faultCode   (:fault-code fault)
                               :faultString (str (:fault-string fault))}]
             (elem :fault [(value-elem fault-struct)])))

  Object
  (response-elem [value] (elem :params [(elem :param [(value-elem value)])])))


(defn unparse
  "returns wraps the content nodes in the methodResponse elements."
  [value]
  (elem :methodResponse [(response-elem value)]))



