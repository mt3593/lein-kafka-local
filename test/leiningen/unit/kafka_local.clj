(ns leiningen.unit.kafka-local
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [leiningen.kafka-local :refer :all]
            [midje.sweet :refer :all]
            [rest-cljer.core :refer [rest-driven]]))

(defn url+ [& suffix]
  (apply str (format "http://localhost:%s" (env :restdriver-port)) suffix))

(defn exists
  [path]
  (-> (io/file path)
      .exists))

(fact-group
 :unit

 (fact "Download from url"
       (rest-driven
        [{:method :GET
          :url "/test.file"}
         {:status 200
          :body (slurp "test/resources/test.download")}]
        (download-kafka (url+ "/test.file"))
        (exists (str kafka-directory "/kafka.tar.gz")) => true))

 (fact "Extract tar.gz"
       (let [local-kafka (str kafka-directory "/kafka.tar.gz")]
         (io/copy (io/file "test/resources/test.tar.gz") (io/file local-kafka))
         (unpack-tgz-file local-kafka)
         (exists (str kafka-directory "/dir1")) => true
         (exists (str kafka-directory "/dir1/file1.txt")) => true
         (slurp (str kafka-directory "/dir1/file1.txt")) => "hello\n"
         (exists (str kafka-directory "/dir1/file2.txt")) => true
         (slurp (str kafka-directory "/dir1/file2.txt")) => "some data\n"
         (exists (str kafka-directory "/dir2")) => true
         (exists (str kafka-directory "/dir2/file3.txt")) => true
         (slurp (str kafka-directory "/dir2/file3.txt")) => "file 3 data!!\n")))
