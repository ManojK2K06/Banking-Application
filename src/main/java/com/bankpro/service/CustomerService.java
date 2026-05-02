package com.bankpro.service;

import com.bankpro.model.Customer;
import com.bankpro.model.Party;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backward-compatibility shim — delegates to PartyService.
 * New code should use PartyService directly.
 */
public class CustomerService {
    private static CustomerService instance;
    private CustomerService() {}
    public static synchronized CustomerService getInstance() {
        if (instance == null) instance = new CustomerService();
        return instance;
    }

    public Customer createCustomer(Customer c) throws Exception {
        Party p = toParty(c);
        PartyService.getInstance().createParty(p);
        c.setId(p.getId());
        c.setCustomerId(p.getPartyId());
        return c;
    }

    public void updateCustomer(Customer c) throws Exception {
        Party p = toParty(c);
        p.setId(c.getId());
        p.setPartyId(c.getCustomerId());
        PartyService.getInstance().updateParty(p);
    }

    public void updateKycStatus(int id, String status) throws Exception {
        PartyService.getInstance().updateKycStatus(id, status);
    }

    public void deactivateCustomer(int id) throws Exception {
        PartyService.getInstance().deactivateParty(id);
    }

    public Customer getById(int id) throws SQLException {
        Party p = PartyService.getInstance().getById(id);
        return p != null ? toCustomer(p) : null;
    }

    public Customer getByCustomerId(String cid) throws SQLException {
        Party p = PartyService.getInstance().getByPartyId(cid);
        return p != null ? toCustomer(p) : null;
    }

    public List<Customer> searchCustomers(String q) throws SQLException {
        return PartyService.getInstance().searchParties(q)
            .stream().map(this::toCustomer).collect(Collectors.toList());
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return PartyService.getInstance().getAllParties()
            .stream().map(this::toCustomer).collect(Collectors.toList());
    }

    public Customer binarySearchByCustomerId(List<Customer> sorted, String id) {
        List<Party> parties = sorted.stream().map(this::toParty).collect(Collectors.toList());
        Party found = PartyService.getInstance().binarySearchByPartyId(parties, id);
        return found != null ? toCustomer(found) : null;
    }

    public int getTotalCustomerCount() throws SQLException {
        return PartyService.getInstance().getTotalPartyCount();
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private Party toParty(Customer c) {
        Party p = new Party();
        p.setPartyType(Party.INDIVIDUAL);
        p.setFirstName(c.getFirstName());
        p.setLastName(c.getLastName());
        p.setDateOfBirth(c.getDateOfBirth());
        p.setEmail(c.getEmail());
        p.setPhone(c.getPhone());
        p.setAddress(c.getAddress());
        p.setCity(c.getCity());
        p.setState(c.getState());
        p.setPincode(c.getPincode());
        p.setCountry(c.getCountry());
        p.setAadharNumber(c.getAadharNumber());
        p.setPanNumber(c.getPanNumber());
        p.setKycStatus(c.getKycStatus());
        p.setCreditScore(c.getCreditScore());
        return p;
    }

    private Customer toCustomer(Party p) {
        Customer c = new Customer();
        c.setId(p.getId());
        c.setCustomerId(p.getPartyId());
        c.setFirstName(p.getFirstName() != null ? p.getFirstName() : p.getCompanyName());
        c.setLastName(p.getLastName() != null ? p.getLastName() : "");
        c.setDateOfBirth(p.getDateOfBirth());
        c.setEmail(p.getEmail());
        c.setPhone(p.getPhone());
        c.setAddress(p.getAddress());
        c.setCity(p.getCity());
        c.setState(p.getState());
        c.setPincode(p.getPincode());
        c.setCountry(p.getCountry());
        c.setAadharNumber(p.getAadharNumber());
        c.setPanNumber(p.getPanNumber());
        c.setKycStatus(p.getKycStatus());
        c.setCreditScore(p.getCreditScore());
        c.setActive(p.isActive());
        c.setCreatedAt(p.getCreatedAt());
        return c;
    }
}
