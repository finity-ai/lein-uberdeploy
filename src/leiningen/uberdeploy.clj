(ns leiningen.uberdeploy
  "Build and deploy uberjar to remote repository."
  (:require [cemerick.pomegranate.aether :as aether]
            [leiningen.core.main :as main]
            [leiningen.pom :as pom]
            [leiningen.deploy :refer [sign-for-repo? signing-opts
                                      signatures-for-artifacts
                                      warn-missing-metadata repo-for]]
            [leiningen.uberjar :as uberjar]
            [clojure.java.shell :as sh]
            [clojure.string :as string]))


(defn- abort-message [message]
  (cond (re-find #"Return code is 405" message)
        (str message "\n" "Ensure you are deploying over SSL.")
        (re-find #"Return code is 401" message)
        (str message "\n" "See `lein help deploying` for an explanation of how"
             " to specify credentials.")
        :else message))


(defn files-for [project repo]
  (let [signed? (sign-for-repo? repo)
        ;; If pom is put in "target/", :auto-clean true will remove it if the
        ;; jar is created afterwards. So make jar first, then pom.
        artifacts {[:extension "jar"] (uberjar/uberjar project)
                   [:extension "pom"] (pom/pom project)}
        sig-opts (signing-opts project repo)]
    (if (and signed? (not (.endsWith (:version project) "-SNAPSHOT")))
      (reduce merge artifacts (signatures-for-artifacts artifacts sig-opts))
artifacts)))


(defn- in-branches [branches]
  (-> (sh/sh "git" "rev-parse" "--abbrev-ref" "HEAD")
      :out
      butlast
      string/join
      branches
      not))


(defn- fail-on-empty-project [project]
  (when-not (:root project)
    (main/abort "Couldn't find project.clj, which is needed for deploy task")))


(defn ^:no-project-needed uberdeploy
  "Deploy uberjar and pom to remote repository.

The target repository will be looked up in :repositories in project.clj:

  :repositories [[\"snapshots\" \"https://internal.repo/snapshots\"]
                 [\"releases\" \"https://internal.repo/releases\"]
                 [\"alternate\" \"https://other.server/repo\"]]

If you don't provide a repository name to deploy to, either \"snapshots\" or
\"releases\" will be used depending on your project's current version. You may
provide a repository URL instead of a name.

See `lein help deploying` under \"Authentication\" for instructions on
how to configure your credentials so you are not prompted on each
deploy."
  ([project]
     (uberdeploy project (if (pom/snapshot? project)
                           "snapshots"
                           "releases")))
  ([project repository]
     (fail-on-empty-project project)
     (let [branches (set (:deploy-branches project))]
       (when (and (seq branches)
                  (in-branches branches))
         (apply main/abort "Can only deploy from branches listed in"
                ":deploy-branches:" branches)))
     (warn-missing-metadata project)
     (let [repo (repo-for project repository)
           files (files-for project repo)]
       (try
         (main/debug "Deploying" files "to" repo)
         (aether/deploy
          :coordinates [(symbol (:group project) (:name project))
                        (:version project)]
          :artifact-map files
          :transfer-listener :stdout
          :repository [repo])
         (catch Exception e
           (when main/*debug* (.printStackTrace e))
           (main/abort (abort-message (.getMessage e))))))))
