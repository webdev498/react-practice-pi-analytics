// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import type from "redux";
import {Observable} from "rxjs/Observable";
import "rxjs/add/observable/dom/ajax";
import Authentication from "./Authentication";
import decamelizeKeysDeep from "decamelize-keys-deep";
import {OuterUrls} from "./Urls";

export type Request = {
  url: string,
  method: "POST" | "GET" | "PUT" | "PATCH" | "DELETE",
  body? : Object
}

export const Methods = {
  POST: "POST",
  GET: "GET",
  PUT: "PUT",
  PATCH: "PATCH",
  DELETE: "DELETE",
}

export const fetch = (request: Request): Observable => {
  return Observable.ajax(decorateRequest(request))
    .catch(error => {
      if (error.status === 401) {
        window.location = OuterUrls.login;
      } else {
        throw error;
      }
    })
}

export const post = (url: string, payload: Object) => fetch(
  {
    url: url,
    method: Methods.POST,
    body: payload && payload.request ? payload.request : {}
  });

export const get = (url: string) => fetch(
  {
    url: url,
    method: Methods.GET,
    body: {}
  });

/**
 * Takes an Rx.DOM.ajax() settings object and returns a clone of the settings with Citation Eagle
 * additions applied to it.
 *
 * See https://github.com/Reactive-Extensions/RxJS-DOM/blob/master/doc/operators/ajax.md#rxdomajaxsettings
 * for setting options.
 */
const decorateRequest = (request: Request): Object => {
  if (request.body && Authentication.user && Authentication.user.length > 0) {
    request.body.requestedBy = Authentication.user;
  }
  return Object.assign({}, request, {
    responseType: "json",
    crossDomain: true,
    body: JSON.stringify(decamelizeKeysDeep(request.body))
  });
}
