(ns server
  (:require [clojure.core.async :refer [go]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [redirect]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-css include-js]]
            [mtg]))

(load "mtg")

(defn imageurl [multiverseid]
  (format "https://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=%d&type=card" multiverseid))

(defn cardimage [multiverseid]
  [:img.cardimg {:src (imageurl multiverseid)}])

(defn stats [power toughness damage]
  (let [remToughness (if (some? damage) (- toughness damage) toughness)
        styleToughness (if (> toughness remToughness) :div.stat.down :div.stat)]
    [:div.stats [:div.stat (format "%d" power)] "/" [styleToughness (format "%d" remToughness)]]))

; todo: permanent is overused, also for sorcery cards on stack for example
; todo: all this 'get-in' can be destructuring of maps instead
(defn permanent [cardinstance]
  (let [eid (get-in cardinstance [:instance/eid])
        mid (get-in cardinstance [:instance/card :card/multiverseid])
        target (get-in cardinstance [:effect/target :db/id])
        power (get-in cardinstance [:instance/card :creature/power])
        toughness (get-in cardinstance [:instance/card :creature/toughness])
        damage (get-in cardinstance [:instance/damage])]
    [:div.card {:id eid :target target} (cardimage mid) (if (some? power) (stats power toughness damage) "")]))

; cards is a list of cardinstances with eid and mid
(defn cardlist [cards]
  [:div.list (map permanent cards)])

; FOR NOW lands and _other_ permanents are separated in this simple way
(defn battlefield [player]
  (let [cards (mtg/zoneinfo player :battlefield [{:instance/card [:card/multiverseid {:card/type [:db/ident]} :creature/power :creature/toughness]} :instance/damage])
        lands (filter #(mtg/has-type? :land %) cards)
        permanents (remove #(mtg/has-type? :land %) cards)]
    (html5
     [:div.permanents (map permanent permanents)]
     [:div.lands (map permanent lands)])))

(comment
  (mtg/zoneinfo "Player1" :battlefield [{:instance/card [:card/multiverseid {:card/type [:db/ident]} :creature/power :creature/toughness]} :instance/damage])
  (battlefield "Player1"))

(defn graveyard [player]
  (cardlist (mtg/zoneinfo player :graveyard [{:instance/card [:card/multiverseid]}])))

(comment
  (graveyard "Player1"))

(defn stack []
  (html5 [:button {:hx-get "/resolve"
                   :hx-select ".mtg"
                   :hx-target "closest .mtg"
                   ; todo: card preview will flicker!
                   :hx-swap "outerHTML"} 'RESOLVE]
         (cardlist (mtg/stack [{:instance/card [:card/multiverseid]} :effect/target]))))

(comment
  (stack))

(defn player [name]
  (let [[[eid lifetotal]] (mtg/player-info name)]
    [:div {:id eid} name [:div.lifetotal lifetotal]]))

(defn index []
  (let [player1 "Player1" player2 "Player2"]
    (html5 [:head
            [:title "Magic: the Gathering"]
            (include-css "css/style.css")
            (include-js "https://unpkg.com/hyperscript.org@0.9.12")
            (include-js "https://unpkg.com/htmx.org@1.9.12/dist/htmx.min.js")]
           [:body
            [:h1 {:class "button" :hx-get "/test"} "MTG"]
            [:div.mtg {:_ "on mouseover in .cardimg put its src into .preview.src"}
             [:div.players "players"
              [:div.player (player player2)]
              [:img.preview {:src (imageurl 0)}]
              [:div.player (player player1)]]
             [:div.graveyards "graveyards"
              [:div.graveyard (graveyard player2)]
              [:div.graveyard (graveyard player1)]]
             [:div.battlefields "battlefields"
              [:div.battlefield.opponent (battlefield player2)]
              [:div.battlefield {:_ "on click in .card tell it toggle .tapped"} (battlefield player1)]]
             [:div.stack {:_ "on mouseover in .card tell it set :target to @target then toggle .highlighted on <div[id=\"$:target\"]/> until mouseout"} "stack" (stack)]]])))

(defn resolve-stack [] (mtg/resolve-stack) (redirect "/"))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/resolve" [] (resolve-stack))
  (GET "/test" [] (html5 [:h1 "You got it"]))
  (route/not-found (html5 [:h1 "Page not found"])))

(def app (wrap-resource app-routes "public"))

; runs in microthread, cannot bound twice. for dev
; redefine 'app to get functionality live while server is running!
(go (run-jetty app {:port 8080}))

; for prod
;(run-jetty app {:port 8080})