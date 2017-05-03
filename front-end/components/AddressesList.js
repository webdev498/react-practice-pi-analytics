// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {DataTable, IconButton, TableHeader} from "react-mdl";
import "../styles/main.scss";

type
AddressesListProps = {
  serviceAddress: String,
  addresses: Array,
  onSortServiceAddress: (address: Object) => void,
  onUnsortServiceAddress: (address: Object) => void
}

class AddressesList extends React.Component {
  props: AddressesListProps;

  constructor(props: AddressesListProps) {
    super(props)
  }

  renderLawFirmId(isLawFirm: boolean, lawFirmId: String): React.Element<any> {
    if (isLawFirm) {
      var href = "".concat("dataServices/firmInfo.jsp?id=", lawFirmId, "#agents");
      return <span>{lawFirmId}&nbsp;<a href={href} target="_blank"><IconButton accent name="open_in_new"/></a></span>
    }
    return <span style={{color: "#bbb"}}>Non law firm</span>
  }

  renderEntity(address: Object): React.Element<any> {
    if (address.isLawFirm) {
      return <a href="#" onClick={(event: Event) => {
        event.preventDefault();
        this.props.onSortServiceAddress(address);
      }}>{address.entity}</a>
    }
    return <span>{address.entity}</span>
  }

  renderServiceAddressWithInclusions(value: String, testValue: String): React.Element<Layout> {
    if (!testValue || testValue.length == 0) {
      return <p>{value}</p>
    }
    var result = new String(value);
    testValue
      .replace(/[;:.,]/g, " ")
      .replace(/  /g, " ")
      .split(" ")
      .forEach((item) => {
        if (value.indexOf(item) >= 0) {
          result = result.split(item).join("<span class='highlighted'>" + item + "</span>");
        }
      });
    return <p dangerouslySetInnerHTML={{__html: result}}/>;
  }

  renderWebsite(website: String): ?React.Element<any> {
    if (website) {
      return <a href={website} target="_blank"><IconButton name="public" accent/></a>
    }
    return null
  }

  renderActions(address: Object) {
    return <a href={lawFirmId}><IconButton name="cancel" accent onClick={(event: Event) => {
      event.preventDefault();
      this.props.onUnsortServiceAddress(address);
    }}/></a>
  }

  mapRows() {
    return this.props.addresses.map(address => ({
      id: address.lawFirmId,
      lawFirmId: this.renderLawFirmId(address.isLawFirm, address.lawFirmId),
      entity: this.renderEntity(address),
      serviceAddress: this.renderServiceAddressWithInclusions(address.serviceAddress, this.props.serviceAddress),
      website: this.renderWebsite(address.website),
      serviceAddressId: address.serviceAddressId,
      actions: this.renderActions(address)
    }))
  };

  render() {

    return (
      <DataTable style={{width: "100%"}} rowKeyColumn="id" rows={this.mapRows()}>
        <TableHeader name="lawFirmId">Law Firm ID</TableHeader>
        <TableHeader name="entity">Firm</TableHeader>
        <TableHeader name="serviceAddress">Service Addresses</TableHeader>
        <TableHeader name="website">www</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
        <TableHeader name="actions">Re-sort</TableHeader>
      </DataTable>
    )
  }
}

export default AddressesList
