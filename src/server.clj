(ns server
  (:require [clojure.core.async :refer [go]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [hiccup.page :refer [html5 include-css include-js]]
            [mtg]))

(load "mtg")

(defn imageurl [multiverseid]
  (format "https://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=%d&type=card" multiverseid))

(defn cardimage [multiverseid]
  (html5 [:img.card {:src (imageurl multiverseid)}]))

(defn firstheader [multiverseid]
  (html5 [:img.card.firstheader {:src (imageurl multiverseid)}]))
(defn cardheader [multiverseid]
  (html5 [:img.card.header {:src (imageurl multiverseid)}]))
(defn stacktop [multiverseid]
  (html5 [:img.card.stacktop {:src (imageurl multiverseid)}]))

; cards is a list of multiverseids
(defn cardlist [cards]
  (let [head (first cards)
        rest (drop 1 (take (- (count cards) 1) cards))
        last (last cards)]
    (if (= 0 (count cards)) ""
        (if (= 1 (count cards)) (html5 [:div.list (cardimage (get-in last [:instance/card :card/multiverseid]))])
            (html5 [:div.list
                    (firstheader (get-in head [:instance/card :card/multiverseid]))
                    (map #(cardheader (get-in % [:instance/card :card/multiverseid])) rest)
                    (stacktop (get-in last [:instance/card :card/multiverseid]))])))))


; FOR NOW lands and _other_ permanents are separated in this simple way
(defn battlefield [player]
  (let [cards (mtg/zoneinfo player :battlefield [{:instance/card [:card/multiverseid {:card/type [:db/ident]}]}])
        lands (filter #(mtg/has-type? :land %) cards)
        permanents (remove #(mtg/has-type? :land %) cards)]
    (html5
     [:div.permanents
      (map #(cardimage (get-in % [:instance/card :card/multiverseid])) permanents)]
     [:div.lands
      (map #(cardimage (get-in % [:instance/card :card/multiverseid])) lands)])))

(comment
  (battlefield "Player1"))

(defn graveyard [player]
  (cardlist (mtg/zoneinfo player :graveyard [{:instance/card [:card/multiverseid]}])))

(comment
  (graveyard "Player1"))

(defn stack []
  (cardlist (mtg/mapmids (mtg/stack))))

(comment
  (stack))

(defn index []
  (let [player1 "Player1" player2 "Player2"]
    (html5 [:head
            [:title "Magic: the Gathering"]
            (include-css "css/style.css")
            (include-js "https://unpkg.com/hyperscript.org@0.9.12")
            (include-js "https://unpkg.com/htmx.org@1.9.12/dist/htmx.min.js")]
           [:body
            [:h1 {:class "button" :hx-get "/test"} "MTG"]
            [:div.mtg {:_ "on mouseover in .card put its src into .preview.src"}
             [:div.players "players"
              [:div.player player2]
              [:img.preview]
              [:div.player player1]]
             [:div.graveyards "graveyards"
              [:div.graveyard (graveyard player2)]
              [:div.graveyard (graveyard player1)]]
             [:div.battlefields "battlefields"
              [:div.battlefield.opponent (battlefield player2)]
              [:div.battlefield {:_ "on click in .card tell it toggle .tapped"} (battlefield player1)]]
             [:div.stack "stack" (stack)]]])))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/res" [] (resource-response "index.html"))
  (GET "/test" [] (html5 [:h1 "You got it"]))
  (route/not-found (html5 [:h1 "Page not found"])))

(def app (wrap-resource app-routes "public"))

; runs in microthread, cannot bound twice. for dev
; redefine 'app to get functionality live while server is running!
(go (run-jetty app {:port 8080}))

; for prod
;(run-jetty app {:port 8080})