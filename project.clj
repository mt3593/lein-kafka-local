(defproject lein-kafka-local "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  :dependencies [[environ "1.0.0"]
                 [org.apache.commons/commons-compress "1.10"]]

  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [rest-cljer "0.1.22"]]
                   :plugins [[lein-midje "3.1.3"]]
                   :resource-paths ["test/resources"]
                   :env {:restdriver-port "8081"}}}

  :plugins [[lein-environ "0.5.0"]]

  :env {:kafka-download-url "http://apache.mirror.anlx.net/kafka/%s/kafka_%s-%s.tgz"
        :scala-version "2.9.1"
        :kafka-version "0.8.2.1"})
