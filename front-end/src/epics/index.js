// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {combineEpics} from "redux-observable";
import {
  assignServiceAddress,
  createLawFirm,
  fetchNextUnsortedServiceAddress,
  getCurrentUser,
  preFetchNextUnsortedServiceAddress,
  searchLawFirm,
  setServiceAddressAsNonLawFirm,
  skipServiceAddress,
  undoServiceAddress,
  unsortServiceAddress
} from "./Backend";

const rootEpic = combineEpics(
  createLawFirm,
  searchLawFirm,
  getCurrentUser,
  skipServiceAddress,
  undoServiceAddress,
  assignServiceAddress,
  unsortServiceAddress,
  setServiceAddressAsNonLawFirm,
  fetchNextUnsortedServiceAddress,
  preFetchNextUnsortedServiceAddress
);

export default rootEpic;
