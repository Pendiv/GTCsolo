// Retain saved item IDs, but hide retired intermediates from JEI.
JEIEvents.hideItems(event => {
    ["gtcsolo:uncured_polyamic_acid_film", "gtcsolo:uncured_boron_nitride_polyamic_acid_film"].forEach(id => event.hide(id));
});
