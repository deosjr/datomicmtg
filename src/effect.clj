(ns effect
  (:require [datomic.client.api :as d]
            [db]))

; todo: targets can get much more complex!
(def effect-schema [{:db/ident :effect/target
                     :db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/one
                     :db/doc "The target of the effect"}])

(d/transact db/conn {:tx-data effect-schema})

(defn damage-player [targeteid amount]
  (let [{currentlife :player/life} (d/pull (d/db db/conn) [:player/life] targeteid)
        newlife (- currentlife amount)]
    [:db/cas targeteid :player/life currentlife newlife]))

(defn damage-creature [targeteid amount]
  (let [{currentdmg :instance/damage} (d/pull (d/db db/conn) [:instance/damage] targeteid)
        ; can't use :default 0 because of db/cas
        actual (if (some? currentdmg) currentdmg 0)
        newdmg (+ actual amount)]
    [:db/cas targeteid :instance/damage currentdmg newdmg]))

(defn damage-any [targeteid amount]
  (let [{lifetotal :player/life} (d/pull (d/db db/conn) [:player/life] targeteid)]
    (if (some? lifetotal)
      (damage-player targeteid amount)
      (damage-creature targeteid amount))))