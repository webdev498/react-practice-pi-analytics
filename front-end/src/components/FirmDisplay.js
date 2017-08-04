// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react"
import {DataTable, IconButton, Layout, TableHeader} from "react-mdl"
import {OuterUrls} from "../services/Urls"

type
FirmDisplayProps = {
  value: Object
}

const FirmDisplay = (props: FirmDisplayProps): React.Element<Layout> => {

  const addressToSort = props.value.serviceAddressToSort

  const renderServiceAddressCell = (value: Object) => {
    if (!value || (!value.name && !value.address)) {
      return <span>N/A</span>
    }
    const nameSearchUrl = OuterUrls.googleSearch.concat(value.name.replace(/ /g, "+"))
    if (value.address) {
      const addressSearchUrl = OuterUrls.googleSearch.concat(value.address.replace(/ /g, "+"))
      return <span>
        <span>{value.name} <a href={nameSearchUrl} target="_blank"><IconButton accent name="search"/></a></span>
        <span>{value.address} <a href={addressSearchUrl} target="_blank"><IconButton accent name="search"/></a></span>
      </span>
    }
    return <span>{value.name} <a href={nameSearchUrl} target="_blank"><IconButton accent name="search"/></a></span>
  }

  const renderCountryEditIcon = (country: string, localId: string) => {
    const url = OuterUrls.country.concat(localId)
    return (
      <span>{country} <a href={url}><IconButton accent name="edit"/></a></span>
    )
  }

  const mapRows = () =>
    [addressToSort].map(address => ({
      lawFirmId: address.lawFirmId,
      name: {name: address.name, address: address.address},
      phone: address.phone,
      country: renderCountryEditIcon(address.country, address.serviceAddressId),
      serviceAddressId: address.serviceAddressId,
    }))

  return (
    <div>
      {props.value.enTranslation ? (
        <h5>{props.value.enTranslation}</h5>
      ) : (null)}
      <DataTable className="firm-display" style={{width: "100%"}} rowKeyColumn="lawFirmId" rows={mapRows()}>
        <TableHeader style={{width: "71%"}} name="name" cellFormatter={renderServiceAddressCell}>Service Address</TableHeader>
        <TableHeader style={{width: "7%"}} name="phone">Phone</TableHeader>
        <TableHeader style={{width: "7%"}} name="country">Country</TableHeader>
        <TableHeader style={{width: "7%"}} name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
    </div>
  )
}

export default FirmDisplay