package com.hydrospark.billing.repository;

import com.hydrospark.billing.model.BillLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillLineItemRepository extends JpaRepository<BillLineItem, String> {
    List<BillLineItem> findByBillId(String billId);
}
