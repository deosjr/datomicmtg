(ns effect
  (:require [datomic.client.api :as d]
            [db]))

; todo: targets can get much more complex!
(def effect-schema [{:db/ident :effect/target
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :db/doc "The target of the effect"}])

(d/transact db/conn {:tx-data effect-schema})

; assumes player target for now
(defn damage [targeteid amount]
  (let [{currentlife :player/life} (d/pull (d/db db/conn) [:player/life] targeteid)
        newlife (- currentlife amount)]
    [:db/cas targeteid :player/life currentlife newlife]))