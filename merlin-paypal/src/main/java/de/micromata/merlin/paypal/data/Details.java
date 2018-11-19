package de.micromata.merlin.paypal.data;

import de.micromata.merlin.paypal.utils.PayPalUtils;

import java.beans.Transient;
import java.math.BigDecimal;

/**
 * Details of Amount.
 */
public class Details {
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal subtotal;

    @Transient
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        total = PayPalUtils.add(total, shipping, tax, subtotal);
        return PayPalUtils.roundAmount(total);
    }


    public BigDecimal getShipping() {
        return shipping;
    }

    /**
     * Ensures scale 2.
     *
     * @param shipping
     * @return this for chaining.
     */
    public Details setShipping(BigDecimal shipping) {
        this.shipping = PayPalUtils.roundAmount(shipping);
        return this;
    }

    public Details setShipping(double shipping) {
        return setShipping(new BigDecimal(shipping));
    }

    public BigDecimal getTax() {
        return tax;
    }

    /**
     * Ensures scale 2.
     *
     * @param tax
     * @return this for chaining.
     */
    public Details setTax(BigDecimal tax) {
        this.tax = PayPalUtils.roundAmount(tax);
        return this;
    }

    public Details setTax(double tax) {
        return setTax(new BigDecimal(tax));
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void calculateSubtotal(Transaction transaction) {
        subtotal = BigDecimal.ZERO;
        for (Item item : transaction.getItemList().getItems()) {
            subtotal = PayPalUtils.add(subtotal, item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
    }
}