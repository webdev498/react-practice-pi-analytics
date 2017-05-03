// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {combineEpics} from "redux-observable";
import {
  assignServiceAddress,
  createLawFirm,
  fetchNextUnsortedServiceAddress,
  getCurrentUser,
  searchLawFirm,
  setServiceAddressAsNonLawFirm,
  unsortServiceAddress
} from "./BackendStub";

const rootEpic = combineEpics(
  createLawFirm,
  searchLawFirm,
  getCurrentUser,
  assignServiceAddress,
  unsortServiceAddress,
  setServiceAddressAsNonLawFirm,
  fetchNextUnsortedServiceAddress
)

export default rootEpic;
