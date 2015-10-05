(ns leiningen.kafka-local
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [leiningen.core.main :as main])
  (:import [java.io
            File
            BufferedInputStream
            FileInputStream]
           [java.util.regex Pattern]
           [org.apache.commons.compress.archivers.tar
            TarArchiveInputStream]
           [org.apache.commons.compress.compressors.gzip
            GzipCompressorInputStream]))

(def kafka-download (format (env :kafka-download-url)
                            (env :kafka-version)
                            (env :scala-version)
                            (env :kafka-version)))

(def kafka-directory (str (System/getProperty "user.home") File/separator ".lein-kafka-local"))

(def zookeeper-running (ref false))

(def kafka-running (ref false))

(defn download-kafka
  [url]
  (main/info (format "Downloading kafka: %s" url))
  (let [local-kafka (str kafka-directory File/separator "kafka.tar.gz")]
      (io/make-parents local-kafka)
      (io/copy (io/input-stream (io/as-url url)) (io/as-file local-kafka))
      local-kafka))

(defn unpack-tgz-file
  [tgz-file kafka-directory]
  (main/info (format "Extracting kafka file: %s" tgz-file))
  (with-open [f-input-stream (FileInputStream. tgz-file)
              b-input-stream (BufferedInputStream. f-input-stream)
              gzip-input-stream (GzipCompressorInputStream. b-input-stream)
              tar-input-stream (TarArchiveInputStream. gzip-input-stream)]
    (loop [tar-entry (.getNextTarEntry tar-input-stream)]
      (when tar-entry
        (let [file-location (str kafka-directory "/" (.getName tar-entry))]
          (io/make-parents file-location)
          (when (.isFile tar-entry)
            (with-open [file-output (io/output-stream file-location)]
              (let [size (.getSize tar-entry)
                    data (byte-array size)]
                (.read tar-input-stream data 0 size)
                (.write file-output data 0 size)))))
        (recur (.getNextTarEntry tar-input-stream)))))
  kafka-directory)

(defn get-root
  [path]
  (main/info (format "Looking for kafka root in %s for {:kafka-version: %s :scala-version: %s}"
                     path
                     (env :kafka-version)
                     (env :scala-version)))
  (some->> path
           io/file
           .list
           (filter (partial re-matches (Pattern/compile (format "kafka_%s-%s" (env :scala-version) (env :kafka-version)))))
           first
           (str path "/")))

(defn make-files-in-bin-executable
  [path]
  (dorun (map #(.setExecutable % true) (.listFiles (io/file (str path "/bin")))))
  path)

(defn- start-process
  [command p]
  (if-not (or @p )
    (dosync (ref-set p (.exec (Runtime/getRuntime) command)))
    (main/info "Already running")))

(defn- stop-process
  [p]
  (when @p
    (dosync
     (.destroy @p)
     (ref-set p false))))

(defn start-zookeeper
  [root-path]
  (main/info "Starting zookeeper")
  (start-process (format "%s/bin/zookeeper-server-start.sh %s/config/zookeeper.properties" root-path root-path) zookeeper-running))

(defn stop-zookeeper
  []
  (stop-process zookeeper-running))

(defn start-kafka
  [root-path]
  (main/info "Starting kafka")
  (start-process (format "%s/bin/kafka-server-start.sh %s/config/server.properties" root-path root-path) kafka-running))

(defn stop-kafka
  []
  (stop-process kafka-running))
