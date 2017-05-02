// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {DataTable, IconButton, TableHeader} from "react-mdl";

type
FirmDisplayProps = {
  firm: Object
}

const FirmDisplay = (props: FirmDisplayProps): React.Element<Layout> => {

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
    return [props.firm].map((firm) => ({
      lawFirmId: firm.lawFirmId,
      entity: firm.entity,
      address: firm.address,
      phone: firm.phone,
      country: renderCountryEditIcon(firm.country, firm.serviceAddressId),
      serviceAddressId: firm.serviceAddressId
    }));
  }

  return (
    <div>
      <DataTable style={{width: "100%"}} rowKeyColumn="lawFirmId" rows={mapRows()}>
        <TableHeader name="entity" cellFormatter={renderGoogleSearchIcon}>Name</TableHeader>
        <TableHeader name="address" cellFormatter={renderGoogleSearchIcon}>Address</TableHeader>
        <TableHeader name="phone">Phone</TableHeader>
        <TableHeader name="country">Country</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
    </div>
  )
}
export  default FirmDisplay