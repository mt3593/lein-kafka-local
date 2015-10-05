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

(defn inspect
  [v]
  (println v)
  v)

(defn delete-directory
  [path]
  (when (exists path)
    (let [file (io/file path)]
      (dorun (map (comp delete-directory (partial str path "/")) (.list file)))
      (io/delete-file file))))

(fact-group
 :unit

 (with-state-changes
   [(before :facts (delete-directory kafka-directory))]

   (fact "Download from url"
         (rest-driven
          [{:method :GET
            :url "/test.file"}
           {:status 200
            :body (slurp "test/resources/test.download")}]
          (download-kafka (url+ "/test.file"))
          (exists (str kafka-directory "/kafka.tar.gz")) => true))

   (fact "Extract tar.gz"
         (io/make-parents (str kafka-directory "/kafka.tar.gz"))
         (let [local-kafka (str kafka-directory "/kafka.tar.gz")]
           (io/copy (io/file "test/resources/test.tar.gz") (io/file local-kafka))
           (unpack-tgz-file local-kafka kafka-directory)
           (exists (str kafka-directory "/dir1")) => true
           (exists (str kafka-directory "/dir1/file1.txt")) => true
           (slurp (str kafka-directory "/dir1/file1.txt")) => "hello\n"
           (exists (str kafka-directory "/dir1/file2.txt")) => true
           (slurp (str kafka-directory "/dir1/file2.txt")) => "some data\n"
           (exists (str kafka-directory "/dir2")) => true
           (exists (str kafka-directory "/dir2/file3.txt")) => true
           (slurp (str kafka-directory "/dir2/file3.txt")) => "file 3 data!!\n")))

 (fact "Get kafka root directory - most up to date version"
       (get-root "test/resources/kafkanames") =>  "test/resources/kafkanames/kafka_2.9.1-0.8.2.1"
       (get-root "test/resources/kafkanames") =>  "test/resources/kafkanames/kafka_3.8.1-0.8.2.1"
       (provided
        (env :scala-version) => "3.8.1"
        (env :kafka-version) => "0.8.2.1")
       (get-root "test/resources/kafkanames") =>  "test/resources/kafkanames/kafka_3.9.1-0.8.2.1"
       (provided
        (env :scala-version) => "3.9.1"
        (env :kafka-version) => "0.8.2.1")
       (get-root "test/resources/kafkanames") =>  "test/resources/kafkanames/kafka_3.9.1-0.8.1.1"
       (provided
        (env :scala-version) => "3.9.1"
        (env :kafka-version) => "0.8.1.1"))

 (with-state-changes
   [(before :contents (-> kafka-download
                          download-kafka
                          (unpack-tgz-file kafka-directory)
                          get-root
                          make-files-in-bin-executable))]

   (with-state-changes
     [(after :facts (do
                    (stop-zookeeper)
                    (stop-kafka)))]

     (fact "Startup zookeeper - no exception should be thrown"
           (start-zookeeper (get-root kafka-directory)))

     (fact "Startup kafka - no exception should be thrown"
           (start-kafka (get-root kafka-directory))))

   (with-state-changes
     [(before :facts (do
                       (start-zookeeper)
                       (start-kafka)))
      (after :facts (do
                    (stop-zookeeper)
                    (stop-kafka)))]

     (future-fact "create topic")))

 (future-fact "topics are created and I can read/write to the topic - end to end test needed so should call the main function"))
