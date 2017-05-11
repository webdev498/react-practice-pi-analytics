// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {DataTable, IconButton, TableHeader} from "react-mdl";

type FirmDisplayProps = {
  value: Object
}

const FirmDisplay = (props: FirmDisplayProps): React.Element<Layout> => {

  let addressToSort = props.value.serviceAddressToSort;

  const renderGoogleSearchIcon = (value: string) => {
    var url = "https://www.google.com/search?q=".concat(value.replace(/ /g, "+"));
    return (
      <span>{value} <a href={url} target="_blank"><IconButton accent name="search"/></a></span>
    )
  }

  const renderCountryEditIcon = (country: string, localId: string) => {
    var url = "addressing/localCountryC.jsp?findLocalEntity=Find&localID=".concat(localId);
    return (
      <span>{country} <a href={url}><IconButton accent name="edit"/></a></span>
    )
  }

  // TODO: Display en translation in second row if service address is not in English
  const mapRows = () => {
    return [addressToSort].map((address) => ({
      lawFirmId: address.lawFirmId,
      name: address.name,
      address: address.address,
      phone: address.phone,
      country: renderCountryEditIcon(address.country, address.serviceAddressId),
      serviceAddressId: address.serviceAddressId
    }));
  }

  return (
    <div>
      {"en" !== addressToSort.languageType ? (
        <h5>{props.value.enTranslation}</h5>
      ) : (null)}
      <DataTable style={{width: "100%"}} rowKeyColumn="lawFirmId" rows={mapRows()}>
        <TableHeader name="name" cellFormatter={renderGoogleSearchIcon}>Name</TableHeader>
        <TableHeader name="address" cellFormatter={renderGoogleSearchIcon}>Address</TableHeader>
        <TableHeader name="phone">Phone</TableHeader>
        <TableHeader name="country">Country</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
    </div>
  )
}
export  default FirmDisplay