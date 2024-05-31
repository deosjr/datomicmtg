(ns effect
  (:require [datomic.client.api :as d]
            [db]))

; todo: targets can get much more complex!
(def effect-schema [{:db/ident :effect/target
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :db/doc "The target of the effect"}])

(d/transact db/conn {:tx-data effect-schema})

(def player-life-q '[:find ?e ?life
                     :in $ ?name
                     :where
                     [?e :player/name ?name]
                     [?e :player/life ?life]])

; assumes player target for now
(defn damage [target amount]
  (let [[[e currentlife]] (d/q player-life-q (d/db db/conn) target)
        newlife (- currentlife amount)]
    [:db/cas e :player/life currentlife newlife]))