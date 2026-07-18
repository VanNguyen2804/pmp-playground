package com.example.pmp.category;

/**
 * PMP_TOPIC is the active, unified study taxonomy derived from PMBOK 8 and the PMA handout.
 * The two legacy values remain so existing databases can be migrated without enum failures.
 */
public enum Taxonomy {
    PMP_TOPIC,
    PMBOK8_DOMAIN,
    PMA_HANDOUT_TOPIC
}
