(ns db
  (:require [datomic.client.api :as d]))

(def client (d/client {:server-type :datomic-local
                       :storage-dir :mem
                       :system "dev"}))

(d/create-database client {:db-name "mtg"})

(comment
  (d/delete-database client {:db-name "mtg"}))

(def conn (d/connect client {:db-name "mtg"}))